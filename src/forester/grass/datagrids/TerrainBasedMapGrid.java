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

import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainGrid;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.texture.Texture;
import forester.grass.GrassPage;
import forester.image.DensityMap;
import java.util.HashMap;

/**
 *
 * @author Kallsta
 */
public class TerrainBasedMapGrid implements MapProvider{

    protected String alphaMapName = "Alpha";
    protected TerrainGrid terrainGrid;
    protected int pageSize;
    protected int numMaps = 1;
    protected boolean advancedTerrain = false;
    
    public TerrainBasedMapGrid(TerrainGrid terrainGrid, int pageSize){
        this.terrainGrid = terrainGrid;
        this.pageSize = pageSize;
    }
    
    public TerrainBasedMapGrid(TerrainGrid terrainGrid, int pageSize, String alphaMapName){
        this(terrainGrid, pageSize);
        this.alphaMapName = alphaMapName;
    }
    
    public TerrainBasedMapGrid(TerrainGrid terrainGrid, int pageSize, boolean advancedTerrain, int numMaps){
        this(terrainGrid,pageSize);
        this.advancedTerrain = advancedTerrain;
        if(numMaps < 1 || numMaps > 3){
            throw new RuntimeException("The possible number of alpha maps in TerrainLighting.j3md is 1 to 3.");
        }
        this.numMaps = numMaps;
    }
    
    @Override
    public HashMap<Integer, DensityMap> getMaps(GrassPage page) {
        Vector3f loc = new Vector3f(page.getX(),0,page.getZ());
        Texture tex = null;
        //Get the terrain quad corresponding to the given page.
        TerrainQuad quad = terrainGrid.getGridTileLoader().getTerrainQuadAt(loc);
        //If there is no terrainquad (tile) corresponding to the given indices,
        //return null.
        if(quad == null){
            return null;
        }
        
        //Ready the hasmap.
        HashMap<Integer, DensityMap> map = new HashMap<Integer,DensityMap>();
        
        for(int i = 0; i < numMaps; i++){
            //If the TerrainLighting.j3md is used, get the alpha maps in order.
            if(advancedTerrain){
                if(i == 0){
                    alphaMapName = "AlphaMap";
                } else if (i == 1){
                    alphaMapName = "AlphaMap_1";
                } else if (i == 2){
                    alphaMapName = "AlphaMap_2";
                }
            }
            tex = quad.getMaterial().getTextureParam(alphaMapName).getTextureValue();
            DensityMap dmap = new DensityMap(tex,pageSize);
            map.put(i, dmap);
        }
        
        return map;
    }
    
}
