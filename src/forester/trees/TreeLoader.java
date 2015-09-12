/*
 * Copyright (c) 2011, Andreas Olofsson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package forester.trees;

import forester.trees.impostors.TreeImpostorGenerator;
import forester.trees.datagrids.TreeData;
import forester.trees.datagrids.TreeDataList;
import forester.trees.datagrids.TreeDataBlock;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.TerrainQuad;
import forester.trees.datagrids.DataGrid;
import forester.trees.datagrids.DataProvider;
import forester.trees.datagrids.MapGrid;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import forester.paging.GeometryPageLoader;
import forester.paging.interfaces.Page;


/**
 * This class is used to load trees.
 * 
 * @author Andreas
 */
public class TreeLoader extends GeometryPageLoader {

    protected ArrayList<TreeLayer> layers;
    protected TreeGeometryGenerator treeGen;
    protected TreeImpostorGenerator treeImpGen;
    protected DataProvider dataProvider;
    protected Terrain terrain;
    protected float terrainHeight;
    protected Vector2f hStore = new Vector2f();
    protected int nIt = Short.MAX_VALUE;

    public TreeLoader(  int tileSize,
                        int resolution,
                        float viewingRange,
                        Node rootNode,
                        Terrain terrain,
                        Camera camera
                     ) 
    {
        super(tileSize, resolution, viewingRange, rootNode, camera, terrain);
        this.terrain = terrain;
        terrainHeight = ((TerrainQuad)terrain).getLocalTranslation().getY();
        
        layers = new ArrayList<TreeLayer>();
        //TODO update here when impostors are added.
        pagingManager.addDetailLevel(viewingRange,0);
        treeGen = new TreeGeometryGenerator();
        init();
    }

    protected final void init() {
        pagingManager.setPageLoader(this);
    }

    public TreeLayer addTreeLayer(Spatial model) {
        return addTreeLayer(model, false);
    }

    public TreeLayer addTreeLayer(Spatial model, boolean usePhysics) {
        TreeLayer layer = new TreeLayer(model, usePhysics);
        layer.setName("TreeLayer" + nIt++);
        layers.add(layer);
        return layer;
    }

    @Override
    public TreePage createPage(int x, int z) {
        Logger.getLogger(TreeLoader.class.getName()).log(Level.INFO, "TreePage created at: ({0},{1})", new Object[]{x, z});
        return new TreePage(x, z, pagingManager);
    }

    @Override
    public Callable<Boolean> loadPage(Page tile) {
        TreePage tTile = (TreePage) tile;
        LoadTask task = new LoadTask(tTile);
        return task;
    }

    public DataProvider getDataProvider() {
        return dataProvider;
    }

    public DataGrid createDataGrid() {
        DataGrid grid = new DataGrid(pagingManager.getPageSize(), pagingManager.getResolution());
        this.dataProvider = grid;
        return grid;
    }
    
    public MapGrid createMapGrid() {
        MapGrid mapGrid = new MapGrid(pagingManager.getPageSize(),this);
        this.dataProvider = mapGrid;
        return mapGrid;
    }

    public void setDataProvider(DataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    public ArrayList<TreeLayer> getLayers() {
        return layers;
    }

    public Terrain getTerrain() {
        return terrain;
    }
    
    public float getTerrainHeight(float x, float z){
        hStore.set(x, z);
        return terrain.getHeight(hStore) + terrainHeight;
    }

    private class LoadTask implements Callable<Boolean> {

        private TreePage page;

        private LoadTask(TreePage page) {
            this.page = page;
        }

        @Override
        public Boolean call() {
            page.createBlocks();
            TreeDataBlock tdb = dataProvider.getData(page);
            if (tdb == null || tdb.isEmpty()) {
                return false;
            }

            page.setBlock(tdb);

            for (int j = 0; j < page.getBlocks().size(); j++) {
                TreeBlock block = (TreeBlock) page.getBlock(j);

                Node[] nodes = new Node[1];
                Node batchNode = new Node("BatchNode_" + page.toString());
                CompoundCollisionShape ccs = null;

                for (int i = 0; i < layers.size(); i++) {
                    TreeLayer layer = layers.get(i);
                    ArrayList<TreeDataList> grid = tdb.get(layer);
                    if (grid == null || grid.isEmpty()) {
                        continue;
                    }
                    TreeDataList dataList = grid.get(j);
                    //Prepare a node for geometry batches.
                    Node model = layer.getModel();
                    //Generate batches for each of the models geometries.
                    for (Spatial spat : model.getChildren()) {
                        Geometry baseGeom = (Geometry) spat;
                        Geometry staticGeometry = treeGen.generateStaticGeometry(baseGeom, dataList, false);
                        if (staticGeometry != null) {
                            batchNode.attachChild(staticGeometry);
                            staticGeometry.setShadowMode(layer.getShadowMode());
                        }
                    }
                    if (layer.isUsePhysics()) {
                        if(ccs == null){
                            ccs = new CompoundCollisionShape();
                        }
                        
                        CompoundCollisionShape temp = layer.getCollisionShape();
                        Vector3f tempLoc = new Vector3f();
                        Vector3f tempScale = new Vector3f();
                        Matrix3f rot = new Matrix3f();
                        for(int h = 0; h < dataList.size(); h++){
                            TreeData data = dataList.get(h);
                            tempScale.set(data.scale,data.scale,data.scale);
                            tempLoc.set(data.x,data.y,data.z);
                            rot.fromAngleNormalAxis(data.rot, Vector3f.UNIT_Y);
                            //Add all shapes.
                            for(ChildCollisionShape s : temp.getChildren()){
                                CollisionShape z = s.shape;
                                z.setScale(tempScale);
                                ccs.addChildShape(z, tempLoc, rot);
                            }
                        }
                    }
                } //For each layer
                //Finalize
                nodes[0] = batchNode;
                block.setNodes(nodes);
                block.calculateRealMax(0);
                block.initPhysics(ccs);
            }//for each block.
            return true;
        }
    }//LoadTask
}//TreeLoader
