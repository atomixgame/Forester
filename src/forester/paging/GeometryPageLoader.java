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
package forester.paging;

import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.TerrainGrid;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import forester.paging.interfaces.PagingManager;
import forester.paging.interfaces.Page;
import forester.paging.interfaces.PageLoader;
import forester.paging.terraingrid.TerrainGridPagingManager;

/**
 * Default PageLoader implementation.
 * 
 * @author Andreas
 */
public class GeometryPageLoader implements PageLoader{

    protected String name;
    protected PagingManager pagingManager;
    
    public GeometryPageLoader(  int pageSize,
                                int resolution,
                                float viewingRange,
                                Node rootNode,
                                Camera camera,
                                Terrain terrain) {
        if(terrain instanceof TerrainGrid){
            TerrainGrid terrainGrid = (TerrainGrid)terrain;
            this.pagingManager = new TerrainGridPagingManager(terrainGrid,pageSize,resolution,rootNode,camera);
        } else {
            this.pagingManager = new GeometryPagingManager(pageSize, resolution, viewingRange, rootNode, camera);
        }
    }
    
    @Override
    public Callable<Boolean> loadPage(Page tile) {
        return null;
    }

    @Override
    public void update(float tpf) {
        pagingManager.update(tpf);
    }

    @Override
    public Page createPage(int x, int z) {
        Logger.getLogger(GeometryPageLoader.class.getName()).log(Level.INFO, "Page created at: ({0},{1})", new Object[]{x, z});
        return new GeometryPage(x,z,pagingManager);
        
    }
    
    public PagingManager getPagingEngine() {
        return this.pagingManager;
    }
    
    /**
     * Turns the visibility of the grass on and off (attaches/detaches it from
     * the scenegraph).
     * 
     * @param visible 
     */
    public void setGlobalVisibility(boolean visible) {
        pagingManager.setVisible(visible);
    }

    public void setFarViewingDistance(float distance) {
        pagingManager.getDetailLevels().remove(0);
        pagingManager.addDetailLevel(distance, distance);
    }

    public int getPageSize() {
        return pagingManager.getPageSize();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GeometryPageLoader other = (GeometryPageLoader) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = this.name != null ? this.name.hashCode() : 0;
        return hash;
    }
    
}//GeometryTileLoader
