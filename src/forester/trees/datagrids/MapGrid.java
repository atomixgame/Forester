/*
 * Copyright (c) 2012, Andreas Olofsson
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
package forester.trees.datagrids;

import com.jme3.math.FastMath;
import com.jme3.texture.Texture;
import forester.RectBounds;
import forester.random.FastRandom;
import forester.trees.TreeLayer;
import forester.trees.TreeLoader;
import forester.trees.TreeBlock;
import forester.trees.TreePage;
import forester.image.DensityMap;
import java.util.ArrayList;
import java.util.HashMap;
import forester.paging.grid.GenericCell2D;
import forester.paging.grid.Grid2D;


/**
 * This class supplies the treeloader with treedata generated from
 * densitymaps.
 * 
 * @author Andreas
 */
public class MapGrid implements DataProvider {

    public enum Scaling {Linear, Quadratic, Linear_Inverted, Quadratic_Inverted}
    protected Scaling scaling = Scaling.Linear;
    
    protected float threshold = 0;
    protected boolean binary = false;
    
    protected TreeLoader treeLoader;
    protected Grid2D<MapCell> grid;
    protected int pageSize;

    public MapGrid(int pageSize, TreeLoader treeLoader) {
        this.pageSize = pageSize;
        this.treeLoader = treeLoader;
        grid = new Grid2D<MapCell>();
    }

    @Override
    public TreeDataBlock getData(TreePage page) {
        TreeDataBlock block = new TreeDataBlock();
        ArrayList<TreeLayer> layers = treeLoader.getLayers();
        for (int i = 0; i < layers.size(); i++) {
            TreeLayer layer = layers.get(i);
            Grid2D<TreeDataList> list = generateTreeData(layer, page);
            if (list != null) {
                block.put(layer, list);
            }
        }
        return block;
    }

    protected Grid2D<TreeDataList> generateTreeData(TreeLayer layer, TreePage page) {

        FastRandom random = new FastRandom();
        float scaleDiff = layer.getMaximumScale() - layer.getMinimumScale();
        Grid2D<TreeDataList> tGrid = new Grid2D<TreeDataList>();

        int resolution = treeLoader.getPagingEngine().getResolution();

        MapCell cell = grid.getCell(page.getX(), page.getZ());
        if (cell == null) {
            return null;
        }
        DensityMap densityMap = cell.maps.get(layer.getDmTexNum());
        
        for (int k = 0; k < resolution; k++) {
            for (int j = 0; j < resolution; j++) {
                TreeBlock block = (TreeBlock) page.getBlock(j + resolution * k);
                RectBounds bounds = block.getBounds();
                float width = bounds.getWidth();
                int count = (int) (layer.getDensityMultiplier() * width * width * 0.001f);
                
                //Densitymap coordinates.
                float offsetX = width*(block.getX() + 0.5f);
                float offsetZ = width*(block.getZ() + 0.5f);
                
                TreeDataList dataList = new TreeDataList(j, k);

                for (int i = 0; i < count; i++) {
                    
                    float x = (random.unitRandom() - 0.5f) * width - 0.01f;
                    float z = (random.unitRandom() - 0.5f) * width - 0.01f;
                    
                    float xx = x + offsetX;
                    float zz = z + offsetZ;
                    
                    float d = densityMap.getDensityUnfiltered(xx, zz, layer.getDmChannel());

                    if(scaling == Scaling.Quadratic){
                        d *= d;
                    } else if (scaling == Scaling.Linear_Inverted){
                        d = 1 - d;
                    } else if (scaling == Scaling.Quadratic_Inverted){
                        d = 1 - d*d;
                    }
            
                    if(binary){
                        d = (d < threshold) ? 0 : 1;
                    }
                    
                    if (random.unitRandom() + threshold < d) {

                        TreeData data = new TreeData();
                        data.x = x;
                        data.z = z;
                        data.y = treeLoader.getTerrainHeight(x + block.getCenterPoint().x, z + block.getCenterPoint().z);

                        data.rot = (-0.5f + random.unitRandom())*3.141593f;
                        data.scale = layer.getMinimumScale() + random.unitRandom() * (scaleDiff);
                        dataList.add(data);
                    }
                }
                tGrid.add(dataList);
            }
        }
        return tGrid;
    }

    /**
     * Load a texture as densitymap.
     * 
     * @param tex The texture.
     * @param x The treepage x-index.
     * @param z The treepage z-index.
     * @param index The ordinal of the map (0 for first density map, 1 for second etc.).
     */
    public void addDensityMap(Texture tex, int x, int z, int index) {
        loadMapCell(x, z).addDensityMap(tex, index);
    }

    protected MapCell loadMapCell(int x, int z) {
        MapCell mapCell = grid.getCell(x, z);
        if (mapCell == null) {
            mapCell = new MapCell(x, z);
            grid.add(mapCell);
        }
        return mapCell;
    }
    
    public Scaling getScaling() {
        return scaling;
    }
    
    /**
     * Method for manipulating density-map values. You can change this to
     * modify the density-values in various ways.
     * 
     * @param scaling 
     */
    public void setScaling(Scaling scaling) {
        this.scaling = scaling;
    }

    
    public float getThreshold() {
        return threshold;
    }

    /**
     * Allows you to restrict planting at areas with density lower then
     * the given threshold value (between 0 and 1). For example, if you set
     * threshold to 0.5, no grass will be planted at points where
     * density < 0.5. When using terrain alphamaps for density, this is a good 
     * way to reduce grass-growth in areas where a certain texture is present, 
     * but has a very low blending value (barely visible).
     */
    public void setThreshold(float threshold) {
        this.threshold = FastMath.clamp(threshold, 0, 1f);
    }
    
    /**
     * Use binary density values (0 or 1). This should be used in
     * conjunction with the setThreshold-method. If the threshold
     * is set to 0.5 and binary is used, all density-values below
     * 0.5 are set to 0, and all values equal to or larger then
     * 0.5 are set to 1.
     * 
     * @param binary True if binary values should be used.
     */
    public void setBinary(boolean binary){
        this.binary = binary;
    }

    /**
     * This class is used to store densitymaps.
     */
    protected class MapCell extends GenericCell2D {

        HashMap<Integer,DensityMap> maps;

        protected MapCell(int x, int z) {
            super(x, z);
            maps = new HashMap<Integer,DensityMap>();
        }

        protected void addDensityMap(Texture tex, int idx) {
            DensityMap map = new DensityMap(tex, pageSize);
            maps.put(idx, map);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final GenericCell2D other = (GenericCell2D) obj;
            if (this.hash != other.hashCode()) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return "MapCell: (" + Short.toString(x) + ',' + Short.toString(z) + ')';
        }
    }
}
