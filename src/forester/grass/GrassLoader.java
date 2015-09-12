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
package forester.grass;

import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.TerrainGrid;
import forester.grass.GrassLayer.MeshType;
import forester.grass.datagrids.MapGrid;
import forester.grass.datagrids.MapProvider;
import forester.grass.datagrids.TerrainBasedMapGrid;
import forester.image.DensityMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import forester.paging.GeometryPageLoader;
import forester.paging.interfaces.Block;
import forester.paging.interfaces.Page;

/**
 * This class is used to create grass.
 * 
 * @author Andreas
 */
public class GrassLoader extends GeometryPageLoader {

    protected static final Logger log = Logger.getLogger(GrassLoader.class.getName());
    //List of grass-layers.
    protected ArrayList<GrassLayer> layers;
    protected Vector2f wind;
    protected GrassGeometryGenerator grassGen;
    protected MapProvider mapProvider;
    protected boolean useBinaries;
    protected String binariesDir = ".";
    protected Terrain terrain;

    /**
     * The only constructor.
     * 
     * @param pageSize The pagesize.
     * @param resolution The resolution of the pages (amount of subdivisions).
     * @param farViewingDistance The far viewing distance for the grass.
     * @param fadingRange The distance over which grass is faded out 
     * (in world units).
     * @param terrain A terrain object.
     * @param rootNode The rootNode of the scene.
     * @param camera The camera used for rendering the scene.
     */
    public GrassLoader( int pageSize,
                        int resolution,
                        float farViewingDistance,
                        float fadingRange,
                        Terrain terrain,
                        Node rootNode,
                        Camera camera
                      )
    {
        super(pageSize, resolution, farViewingDistance, rootNode, camera, terrain);
        pagingManager.addDetailLevel(farViewingDistance, fadingRange);
        this.terrain = terrain;
        layers = new ArrayList<GrassLayer>();
        wind = new Vector2f(0, 0);
        grassGen = new GrassGeometryGenerator(terrain);
        init();
    }

    protected final void init() {
        pagingManager.setPageLoader(this);
    }

    @Override
    public Callable<Boolean> loadPage(Page page) {
        return new LoadTask((GrassPage) page);
    }

    @Override
    public void update(float tpf) {
        for (GrassLayer layer : layers) {
            layer.update();
        }
        pagingManager.update(tpf);
    }

    @Override
    public GrassPage createPage(int x, int z) {
        Logger.getLogger(GrassLoader.class.getName()).log(Level.INFO, "GrassTile created at: ({0},{1})", new Object[]{x, z});
        return new GrassPage(x, z, pagingManager);
    }

    /**
     * Creates and returns a mapgrid-object. 
     * 
     * @return The mapgrid.
     */
    public MapGrid createMapGrid() {
        MapGrid grid = new MapGrid(pagingManager.getPageSize());
        this.mapProvider = grid;
        return grid;
    }
    
    public TerrainBasedMapGrid createTerrainBasedMapGrid(){
        TerrainBasedMapGrid tbmg = new TerrainBasedMapGrid((TerrainGrid)terrain,pagingManager.getPageSize());
        return tbmg;
    }
    
    public TerrainBasedMapGrid createTerrainBasedMapGrid(String alphaMapName){
        TerrainBasedMapGrid tbmg = new TerrainBasedMapGrid((TerrainGrid)terrain,pagingManager.getPageSize(), alphaMapName);
        return tbmg;
    }
    
    public TerrainBasedMapGrid createTerrainBasedMapGrid(boolean advancedTerrain, int numMaps){
        TerrainBasedMapGrid tbmg = new TerrainBasedMapGrid((TerrainGrid)terrain,pagingManager.getPageSize(), advancedTerrain, numMaps);
        return tbmg;
    }

    /**
     * Adds a new layer of grass to the grassloader. 
     * 
     * @param material The material for the main geometry.
     * @param type The meshtype of the main geometry.
     * @return A reference to the GrassLayer object.
     */
    public GrassLayer addLayer(Material material, MeshType type) {
        GrassLayer layer = new GrassLayer(material, type, this);
        layers.add(layer);
        return layer;
    }

    //***************************Getters and setters***********************

    public ArrayList<GrassLayer> getLayers() {
        return layers;
    }

    public void setLayers(ArrayList<GrassLayer> layers) {
        this.layers = layers;
    }

    public Vector2f getWind() {
        return wind;
    }

    public void setWind(Vector2f wind) {
        for (GrassLayer layer : layers) {
            layer.setWind(wind);
        }
    }

    public void setUseBinaries(boolean useBinaries) {
        this.useBinaries = useBinaries;
    }

    public void setBinariesDir(String binariesDir) {
        this.binariesDir = binariesDir;
    }

    public MapProvider getMapProvider() {
        return mapProvider;
    }

    public void setMapProvider(MapProvider mapProvider) {
        this.mapProvider = mapProvider;
    }

    public Terrain getTerrain() {
        return terrain;
    }
    
    protected class LoadTask implements Callable<Boolean> {

        GrassPage page;

        protected LoadTask(GrassPage page) {
            this.page = page;
        }

        @Override
        public Boolean call() {

            //Get the density and colormaps.
            HashMap<Integer,DensityMap> densityMaps = mapProvider.getMaps(page);
            if (densityMaps == null) {
                return false;
            }
            if (densityMaps.isEmpty()) {
                return false;
            }

            //Creates the empty page objects.
            page.createBlocks();

            ArrayList<Block> blocks = page.getBlocks();
            //Loads grass geometry to each page.
            for (Block b : blocks) {
                GrassBlock block = (GrassBlock) b;
                Node[] nodes = new Node[1];
                nodes[0] = new Node("Grass");

                for (int i = 0; i < layers.size(); i++) {
                    
                    GrassLayer layer = layers.get(i);
                    DensityMap densityMap = densityMaps.get(layer.getDmTexNum());
                    
                    if (densityMap == null) {
                        continue;
                    }

                    Geometry geom = grassGen.createGrassGeometry(   layer,
                                                                    block,
                                                                    page,
                                                                    densityMap
                                                                );
                    
                    geom.setQueueBucket(Bucket.Transparent);
                    geom.setShadowMode(layer.getShadowMode());
                    nodes[0].attachChild(geom);

                }//for each layer
                block.setNodes(nodes);
                block.calculateRealMax(0);
            }//for each page

            for (Block b : page.getBlocks()) {
                b.calculateRealMax(0);
            }
            return true;
        }//call
    }//LoadTask
    
}//AbstractGrassLoader