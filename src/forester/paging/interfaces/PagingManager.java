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
package forester.paging.interfaces;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import forester.paging.DetailLevel;
import forester.paging.grid.Cell2D;
import forester.paging.grid.Grid2D;

/**
 * This is the pagingmanager interface. Pagingmanagers deals with high-level
 * paging tasks.
 * 
 * @author Andreas
 */
public interface PagingManager {
    
    /**
     * This method connects the manager with a pageloader. It has to 
     * be called before the update method is called.
     * 
     * @param pageLoader The pageloader.
     */
    public void setPageLoader(PageLoader pageLoader);
    
    /**
     * The update method. Should be called every frame.
     * 
     * @param tpf The number of seconds passed since the last frame.
     */
    public void update(float tpf);
        
    /**
     * Getter for blocksize.
     * 
     * @return The blocksize used by the engine.
     */
    public float getBlockSize();
    
    /**
     * Getter for pagesize.
     * 
     * @return The pagesize used by the engine.
     */
    public short getPageSize();
    
    /**
     * Getter for resolution
     * 
     * @return The resolution used by the engine (amount of blocks in each page).
     */
    public short getResolution();
    
    /**
     * Getter for the current cell.
     * 
     * @return The curent cell.
     */
    public Cell2D getCurrentCell();
    
    /**
     * Getter for the camera.
     * 
     * @return The camera used by the paging engine.
     */
    public Camera getCamera();
    
    /**
     * Getter for the executor.
     * 
     * @return The executor object used by the engine.
     */
    public ExecutorService getExecutor();
    
    /**
     * Getter for the pageloader.
     * 
     * @return The pageLoader used by the engine.
     */
    public PageLoader getPageLoader();
    
    /**
     * Getter for the current grid size
     * 
     * @return The amount of cells (pages) in the grid.
     */
    public int getCurrentGridSize();
    
    /**
     * Checks whether or not the paged geometry is visible.
     * 
     * @return Whether or not the paged geometry is visible.
     */
    public boolean isVisible();
    
    /**
     * Gets the paging node. All geometry managed by the pagingsystem
     * is attached to this node.
     * 
     * @return The paging node. 
     */
    public Node getPagingNode();
    
    /**
     * Get the ArrayList containing the detail level objects.
     * 
     * @return The detail level objects.
     */
    public ArrayList<DetailLevel> getDetailLevels();
    
    /**
     * Get the page grid object.
     * 
     * @return The page grid.
     */
    public Grid2D<Page> getGrid();
    
    /**
     * Get the radius of the page grid.
     * 
     * @param radius The radius of the page grid.
     */
    public void setRadius(float radius);
    
    /**
     * Set the pagesize.
     * @param pageSize The pagesize.
     */
    public void setPageSize(int pageSize);
    
    /**
     * Set the resoultion.
     * 
     * @param resolution The resolution of pages (number of subdivisions).
     * The total amount of blocks in a page is equal to resolution^2.
     */
    public void setResolution(int resolution);
    
    /**
     * Check whether or not fading is enabled.
     * 
     * @return True if fading is enabled, false otherwise.
     */
    public boolean isFadeEnabled();
    
    /**
     * Set whether or not to use a built-in cache to store pages for a
     * set period (in ms) before discarding them completely. This mechanic
     * is most useful when using small grids and/or massive pages that takes
     * a significant amount of time to load.
     * 
     * @param useCache Whether or not to use the cache.
     */
    public void setUseCache(boolean useCache);
    
    /**
     * Set the time period that pages should stay alive after being removed from
     * the grid.
     * 
     * @param time The time in seconds.
     */
    public void setCacheTime(float time);
    
    /**
     * Sets the visibility of the paged geometry globally.
     * 
     * @param visible True if visible, false otherwise.
     */
    public void setVisible(boolean visible);
    
    /**
     * Sets the paging node.
     * 
     * @param pagingNode The paging node.
     */
    public void setPagingNode(Node pagingNode);
    
    /**
     * Adds a detail level to the geometry. The engine will do fading- and
     * visibility-calculations based on these.
     * 
     * @param farDist The far viewing-distance of this detail-level.
     * @param fadingRange The length of the fade transition (in world units).
     */
    public void addDetailLevel(float farDist, float fadingRange);
    
    public void reloadPages(float l, float r, float t, float b);
    public void reloadPages(Vector3f center, float radius);
    
    /**
     * Reloads the page corresponding to the given location.
     * 
     * @param loc The location in world space.
     */
    public void reloadPage(Vector3f loc);
    
    /**
     * Reloads a page based on its x and z coordinates.
     * 
     * @param x The x-coordinate of the page.
     * @param z The z-coordinate of the page.
     */
    public void reloadPage(int x, int z);
    
    /**
     * Reloads the whole grid.
     * 
     */
    public void reloadPages();

}//PagingEngine