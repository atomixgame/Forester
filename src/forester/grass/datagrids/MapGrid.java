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
package forester.grass.datagrids;

import com.jme3.texture.Texture;
import forester.grass.GrassPage;
import forester.image.DensityMap;
import java.util.HashMap;
import forester.paging.grid.GenericCell2D;
import forester.paging.grid.Grid2D;

/**
 * The mapgrid is used to provide densitymaps for pageloaders.
 * 
 * @author Andreas
 */
public class MapGrid implements MapProvider {
    
    protected Grid2D<MapCell> grid;
    protected int pageSize;

    public MapGrid(int pageSize) {
        this.pageSize = pageSize;
        grid = new Grid2D<MapCell>();
    }
    
    @Override
    public HashMap<Integer,DensityMap> getMaps(GrassPage page) {
        MapCell mapCell = grid.getCell(page.getX(), page.getZ());
        
        //Return null to set the page as being idle (no processing).
        if(mapCell == null || mapCell.maps.isEmpty()){
            return null;
        }
        return mapCell.maps;
    }
    
    /**
     * Adds a densitymap to the grid.
     * 
     * @param tex The texture.
     * @param x The cell's x-index.
     * @param z The cell's z-index.
     * @param index The ordinal of the map (use 0 for first density map, 1 for second etc.).
     */
    public void addDensityMap(Texture tex, int x, int z, int index){
        loadMapCell(x,z).addDensityMap(tex, index);
    }
    
    /**
     * Adds a densitymap to the grid at index 0.
     * 
     * @param tex The texture.
     * @param x The cell's x-index.
     * @param z The cell's z-index.
     */
    public void addDensityMap(Texture tex, int x, int z){
        addDensityMap(tex,x,z,0);
    }
    
    protected MapCell loadMapCell(int x, int z){
        MapCell mapCell = grid.getCell(x,z);
        if(mapCell == null){
            mapCell = new MapCell(x,z);
            grid.add(mapCell);
        }
        return mapCell;
    }
    
    /**
     * This class is used to store density and colormaps.
     */
    protected class MapCell extends GenericCell2D {
        
        HashMap<Integer,DensityMap> maps;
        
        protected MapCell(int x, int z){
            super(x,z);
            maps = new HashMap<Integer,DensityMap>();
        }
        
        protected void addDensityMap(Texture tex, int idx){
            DensityMap map = new DensityMap(tex,pageSize);
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
        public int hashCode(){
            return hash;
        }
        
        @Override
        public String toString() {
            return "MapCell: (" + Short.toString(x) + ',' + Short.toString(z) + ')';
        }
        
    }//MapCell
    
}
