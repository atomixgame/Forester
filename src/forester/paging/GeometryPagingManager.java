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
package forester.paging;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import forester.paging.grid.Cell2D;
import forester.paging.grid.GenericCell2D;
import forester.paging.grid.Grid2D;
import forester.paging.interfaces.PagingManager;
import forester.paging.interfaces.Page;
import forester.paging.interfaces.PageLoader;

/**
 * This is the base class of all paging engines. This class is used for
 * loading, unloading and processing pages. Tiles are high-level objects
 * used for dividing a space into smaller entities. Each page is in 
 * turn sub-divided into pages. The amount of pages in each page depends
 * on the resolution of the page.
 * 
 * @author Andreas
 */
public class GeometryPagingManager implements PagingManager {
    
    protected static final Logger log = Logger.getLogger(GeometryPagingManager.class.getCanonicalName());
    protected Node pagingNode;
    protected Node rootNode;
    //Used to turn visibility off globally.
    protected boolean visible = true;
    
    protected ArrayList<DetailLevel> detailLevels;
    protected int numDetailLevels;
    protected boolean fadeEnabled = false;
    
    protected Camera camera;
    protected PageLoader pageLoader;
    protected ExecutorService executor;
    
    //The size of a page.
    protected short pageSize;
    protected float blockSize;
    protected short resolution;
    protected float radius;
    protected short gridSize;
    protected short halfGridSize;         //(size - 1) / 2
    
    
    //Grid data
    protected Grid2D<Page> grid;
    protected Grid2D<Page> cache;
    protected boolean useCache = true;
    protected float cacheTime = 6f;
    
    protected Cell2D currentCell;
    
    protected boolean updatePages = false;
    
    //Temporary variable
    protected Vector3f camPos;
    
    
    /**
     * This constructor should generally be avoided, as you need to set other 
     * values manually and may have to re-load the entire grid several times.
     * 
     * @param rootNode The root node of the scene.
     * @param camera camera A jME <code>Camera</code> object. Most grid-calculations are based 
     * on it.
     */
    public GeometryPagingManager(Node rootNode, Camera camera){
        this(512,4,512f,rootNode,camera);
    }
    
    /**
     * Constructor.
     * 
     * @param pageSize The size of pages.
     * @param resolution Each page contains resolution^2 pages.
     * @param radius Used to determine the number of pages in the grid.
     * @param rootNode The root node of the scene.
     * @param camera A jME <code>Camera</code> object. Most grid-calculations are based 
     * on it.
     */
    public GeometryPagingManager(int pageSize, int resolution, float radius, Node rootNode, Camera camera){
        
        this.pageSize = (short) pageSize;
        this.resolution = (short) resolution;
        this.blockSize = (pageSize/(float)resolution);
        
        this.radius = radius;
        this.camera = camera;
        
        this.rootNode = rootNode;
        detailLevels = new ArrayList<DetailLevel>();
        pagingNode = new Node("Paging");
        rootNode.attachChild(pagingNode);
        
        if(useCache){
            //Start at 5*size, expands if needed.
            cache = new Grid2D<Page>(5,gridSize);
        }
    }
    
    @Override
    public void setPageLoader(PageLoader pageLoader)
    {
        if(this.pageLoader == null){
            this.pageLoader = pageLoader;
            initGrid();
        } else {
            this.pageLoader = pageLoader;
            reloadPages();
        }
    }
    
    protected void initGrid(){
        
        //Limit blocksize min, in case weird values are being used.
        if(blockSize < 32f){
            log.log(Level.INFO, "Very small page size (pageSize: {0}); setting to minimum value: 32f.", pageSize);
            this.resolution = (short)(pageSize/16f);
            this.blockSize = (pageSize/(float)resolution);
            
        }
        //Calculate gridsize.
        gridSize = (short) (2*((short)(radius/(float)pageSize) + 1) + 1);
        halfGridSize = (short)((gridSize - 1) / 2);
        //Create a new grid.
        grid = new Grid2D<Page>(gridSize,gridSize);
        log.log(Level.INFO, "Grid created (number of pages: {0}).",gridSize*gridSize);
        
        camPos = camera.getLocation();
        Cell2D camCell = getGridCell(camPos);
        currentCell = camCell;
        
        for (int k = -halfGridSize; k <= halfGridSize; k++) {
            for (int i =  -halfGridSize; i <= halfGridSize; i++) {
                int x = i + camCell.getX();
                int z = k + camCell.getZ();
                Page page = pageLoader.createPage(x, z);
                grid.add(page);
            }
        }
        setVisible(true);
    }
    
    @Override
    public void update(float tpf)
    {
        camPos = camera.getLocation();
        Cell2D camCell = getGridCell(camPos);
        
        //Check if the grid should be scrolled.
        if(camCell.hashCode() != currentCell.hashCode()){
            scrollGrid(camCell);
        }
        
        Page page = null;
        for (int i = 0; i < grid.size(); i++){
            page= grid.get(i);
            if(page == null){
                //DEBUG
                throw new RuntimeException(page.toString() + " is null");
            }
            
            if(!page.isLoaded() && !page.isIdle() && !page.isPending()){
                Callable<Boolean> task = pageLoader.loadPage(page);
                Future<Boolean> future = getExecutor().submit(task);
                page.setFuture(future);
                page.setPending(true); 
                continue;
                
            } else if(page.isPending()){
                if(page.getFuture().isDone()){
                    try {
                        boolean result = page.getFuture().get();
                        if(result == true){
                            page.setLoaded(true);
                        } else {
                            page.setIdle(true);
                        }
                        page.setPending(false);
                        page.setFuture(null);
                    } catch (InterruptedException ex) {
                        log.log(Level.SEVERE, null, ex.getCause());
                    } catch (ExecutionException ex) {
                        log.log(Level.SEVERE, null, ex.getCause());
                    }
                    
                }
            } else if(page.isLoaded()){
                //If the page is loaded, update and process it every frame.
                if(updatePages){
                    page.update(tpf);
                }
                page.process(camPos);
            }
        }
        
        //If the cache is being used.
        if(useCache){
            page = null;
            float delta = tpf;
            for(int i = 0; i < cache.size(); i++){
                page = cache.get(i);
                if(page.getCacheTimer() >= cacheTime){
                    cache.remove(i);
                    page.unload();
                } else {
                    page.increaseCacheTimer(delta);
                }
            }
        }
    }
    
    /**
     * Internal method.
     * 
     * This method is called whenever the camera moves from one grid-cell to
     * another, to move the grid along with the camera.
     */
    protected void scrollGrid(Cell2D camCell)
    {
        int dX = camCell.getX() - currentCell.getX();
        int dZ = camCell.getZ() - currentCell.getZ();
        
        Page page = null;
        
        if (dX == 1 || dX == -1){ 
            int oldX = currentCell.getX() - dX*halfGridSize;
            int newX = oldX + dX*gridSize;
            
            for(int k =  -halfGridSize; k <= halfGridSize; k++){
                int z = k + currentCell.getZ();
                
                if(useCache){
                    //Browse the cache to see if the page is there before
                    //creating a new one
                    page = cache.getCell(newX,z);
                    if(page == null){
                        Page newTile = pageLoader.createPage(newX, z);
                        page = grid.setCell(oldX,z,newTile);
                        cache.add(page);
                        page.resetCacheTimer();
                    } else {
                        Page oldTile = grid.setCell(oldX,z,page);
                        log.log(Level.INFO, "Tile recycled from cache at: {0}", page.toString());
                        cache.remove(page);
                        cache.add(oldTile);
                        oldTile.resetCacheTimer();
                    }
                }
                else{
                    //Just create a new page and loose the old one.
                    Page newTile = pageLoader.createPage(newX, z);
                    Page oldTile = grid.setCell(oldX,z,newTile);
                    oldTile.unload();
                }
            }
            page = null;
        }
        
        if(dZ == 1 || dZ == -1){
            int oldZ = currentCell.getZ() - dZ*halfGridSize;
            int newZ = oldZ + dZ*gridSize;
            
            for(int i =  -halfGridSize; i <= halfGridSize; i++){
                //Check to make sure that this page was not checked in the
                //previous loop.
                if((dX == 1 && i == -halfGridSize) || (dX == -1 && i == halfGridSize)){
                    continue;
                }
                int x = i + currentCell.getX();
                if(useCache){
                    //Browse the cache to see if the page is there before
                    //creating a new one
                    page = cache.getCell(x,newZ);
                    if(page == null){
                        Page newTile = pageLoader.createPage(x, newZ);
                        page = grid.setCell(x,oldZ,newTile);
                        cache.add(page);
                        page.resetCacheTimer();
                    } else {
                        Page oldTile = grid.setCell(x,oldZ,page);
                        log.log(Level.INFO, "Tile recycled from cache at: {0}", page.toString());
                        cache.remove(page);
                        cache.add(oldTile);
                        oldTile.resetCacheTimer();
                    }
                }
                else{
                    Page newTile = pageLoader.createPage(x, newZ);
                    Page oldTile = grid.setCell(x,oldZ,newTile);
                    oldTile.unload();
                }
            }
            page = null;
        }
        currentCell = camCell;
    }
    
    @Override
    public void reloadPages(){
        for(Page page: grid){
            page.unload();
        }
        grid.clear();
        if(useCache){
            cache.clear();
        }
        initGrid();
    }
    
    @Override
    public void reloadPages(float l, float r, float t, float b){
        Vector3f tl = new Vector3f(l,0,t);
        Vector3f br = new Vector3f(r,0,b);
        Cell2D tlc = getGridCell(tl);
        Cell2D brc = getGridCell(br);
        for(int j = brc.getZ(); j <= tlc.getZ();j++){
            for(int i = tlc.getX(); i <= brc.getX();i++){
                reloadPage(i,j);
            }
        }
    }
    
    @Override
    public void reloadPages(Vector3f center, float radius) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void reloadPage(Vector3f loc){
        Cell2D cell = getGridCell(loc);
        reloadPage(cell.getX(),cell.getZ());
    }
    
    @Override
    public void reloadPage(int x, int z){
        Page page = grid.getCell(x, z);
        if(page != null){
            page.unload();
            grid.removeCell(page);
            Page newTile = pageLoader.createPage(x, z);
            grid.add(newTile);
        }
    }
    
    /**
     * A method for getting the grid cell that matches a certain xyz-location.
     * 
     * @param loc The location-vector.
     * @return The cell matching the given location.
     */
    public Cell2D getGridCell(Vector3f loc){
        float x = loc.x;
        float z = loc.z;
        int t = (x >= 0) ? 1 : -1;
        x = x/(float)pageSize + t*0.5f;
        t = (z >= 0) ? 1 : -1;
        z = z/(float)pageSize + t*0.5f;
        return new GenericCell2D((int)x,(int)z);
    }
    
    /**
     * A convenience method for getting cells based on world x and z coordinates.
     * 
     * @param xIn The world x-coordinate.
     * @param zIn The world z-coordinate.
     * @return The cell matching the given location.
     */
    public Cell2D getGridCell(float xIn, float zIn){
        return getGridCell(new Vector3f(xIn,0,zIn));
    }

    @Override
    public PageLoader getPageLoader() {
        return pageLoader;
    }

    @Override
    public short getResolution() {
        return resolution;
    }

    @Override
    public short getPageSize() {
        return pageSize;
    }
    
    @Override
    public float getBlockSize() {
        return blockSize;
    }

    @Override
    public Camera getCamera() {
        return camera;
    }
    
    @Override
    public Cell2D getCurrentCell() {
        return currentCell;
    }
    
    @Override
    public int getCurrentGridSize(){
        return grid.size();
    }

    public float getCacheTime() {
        return cacheTime;
    }

    public boolean isUseCache() {
        return useCache;
    }
    
    @Override
    public void setCacheTime(float cacheTime) {
        if(cacheTime > 60){
            log.log(Level.WARNING,"The cache-time is extremely high, make sure it's correctly typed and measured in seconds.");
        }
        this.cacheTime = cacheTime;
    }

    @Override
    public void setUseCache(boolean useCache) {
        if(useCache == true && this.cache == null){
            cache = new Grid2D<Page>(5,gridSize);
        }
        if(useCache == false && this.cache != null){
            cache = null;
        }
        this.useCache = useCache;
    }

    @Override
    public ExecutorService getExecutor() {
        if(executor == null){
            executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread th = new Thread(r);
                    th.setName("Paging Thread");
                    th.setDaemon(true);
                    return th;
                }
            });
        }
        return executor;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        
        if(visible == true && this.visible == false){
            rootNode.attachChild(pagingNode);
            this.visible = visible;
        }
        
        else if(visible == false && this.visible == true){
            pagingNode.removeFromParent();
            this.visible = visible;
        }
    }
    
    @Override
    public Node getPagingNode(){
        return pagingNode;
    }
    
    @Override
    public void setPagingNode(Node pagingNode){
        this.pagingNode = pagingNode;
    }
    
    public void addDetailLevel(float farDist)
    {
        addDetailLevel(farDist,0);
    }
    
    
    @Override
    public void addDetailLevel(float farDist , float fadingRange)
    {
        float nearDist = 0;
        DetailLevel level = null;
        
        //If a detail level has previously been added, use its far distance as 
        //near distance for the new one.
        if(numDetailLevels != 0){
            level = detailLevels.get(numDetailLevels - 1);
            nearDist = level.farDist;
            if(nearDist >= farDist)
                throw new RuntimeException("The near viewing distance must be closer then the far viewing distance");
        }
        if(fadingRange > 0){
            this.fadeEnabled = true;
        }
        DetailLevel newLevel = new DetailLevel();
        newLevel.setFarDist(farDist);
        newLevel.setNearDist(nearDist);
        newLevel.setTransition(fadingRange);
        detailLevels.add(newLevel);
        numDetailLevels += 1;
        
        if(farDist > radius){
            radius = farDist;
            if(grid != null){
                reloadPages();
            }
        }
    }
   
    public void removeDetailLevels() 
    {
        detailLevels.clear();
        setVisible(false);
    }
    
    public int getNumDetailLevels() {
        return numDetailLevels;
    }

    @Override
    public ArrayList<DetailLevel> getDetailLevels() {
        return detailLevels;
    }

    @Override
    public boolean isFadeEnabled() {
        return fadeEnabled;
    }

    @Override
    public Grid2D<Page> getGrid() {
        return grid;
    }

    @Override
    public void setRadius(float radius) {
        this.radius = radius;
        reloadPages();
        
    }

    @Override
    public void setResolution(int resolution) {
        this.resolution = (short) resolution;
        reloadPages();
    }

    @Override
    public void setPageSize(int pageSize) {
        if(pageSize < 64){
            pageSize = 64;
        }
        this.pageSize = (short) pageSize;
        reloadPages();
    }

    public boolean isUpdatePages() {
        return updatePages;
    }

    public void setUpdatePages(boolean updatePages) {
        this.updatePages = updatePages;
    }
    
} //AbstractPagingEngine
