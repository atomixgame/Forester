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
import java.util.ArrayList;
import java.util.concurrent.Future;
import forester.paging.grid.GenericCell2D;
import forester.paging.interfaces.Block;
import forester.paging.interfaces.PagingManager;
import forester.paging.interfaces.Page;

/**
 * Base class for all geometrypages.
 * 
 * @author Andreas
 */
public class GeometryPage extends GenericCell2D implements Page {

    protected Future<Boolean> future;
    protected float cacheTimer = 0;
    protected int bumps = 0;
    protected ArrayList<Block> blocks;
    protected short resolution;
    protected short pageSize;
    protected float blockSize;
    protected float halfBlockSize;
    protected PagingManager manager;
    protected Vector3f centerPoint;
    protected boolean loaded;
    protected boolean pending;
    protected boolean idle;

    public GeometryPage(int x, int z, PagingManager manager) {
        super(x, z);
        this.manager = manager;
        this.resolution = manager.getResolution();
        this.pageSize = manager.getPageSize();
        this.blockSize = manager.getBlockSize();
        this.halfBlockSize = blockSize*0.5f;
        this.centerPoint = new Vector3f(x * manager.getPageSize(), 0, z * manager.getPageSize());
    }

    @Override
    public void update(float tpf) {
    }

    @Override
    public Block getBlock(int i) {
        return blocks.get(i);
    }

    @Override
    public ArrayList<Block> getBlocks() {
        return blocks;
    }

    @Override
    public void createBlocks() {
        blocks = new ArrayList<Block>(resolution * resolution);

        Block block = null;
        for (int j = 0; j < resolution; j++) {
            for (int i = 0; i < resolution; i++) {
                float posX = (i + 0.5f) * blockSize + (x - 0.5f) * pageSize;
                float posZ = (j + 0.5f) * blockSize + (z - 0.5f) * pageSize;
                Vector3f center = new Vector3f(posX, 0, posZ);
                block = createBlock(i, j, center, manager);
                blocks.add(block);
            }
        }
    }

    @Override
    public void process(Vector3f camPos) {

        for (Block block : blocks) {
            //If the pagingNode is hidden, don't make any visibility
            //calculations.
            if (!manager.isVisible()) {
                return;
            }
            if(block.getNodes() == null){
                continue;
            }
            //Get the distance to the page center.
            float dx = block.getCenterPoint().x - camPos.x;
            float dz = block.getCenterPoint().z - camPos.z;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);            

            ArrayList<DetailLevel> levels = manager.getDetailLevels();
            //Start with the detail-level furthest away
            for (int l = levels.size() - 1; l >= 0; l--) {
                if(block.getNode(l).getChildren().isEmpty()){
                    continue;
                }
                DetailLevel thisLvl = levels.get(l);
                DetailLevel nextLvl = null;

                if (l > 0) {
                    nextLvl = levels.get(l - 1);
                }

                boolean vis = false;

                boolean fadeEnable = false;
                float fadeStart = 0;
                float fadeEnd = 0;

                //Standard visibility check.
                if (dist < thisLvl.farDist && dist >= thisLvl.nearDist) {
                    vis = true;
                }
                
                if (manager.isFadeEnabled()) {

                    //This is the diameter of the (smallest) circle enclosing
                    //the page and all its geometry in the xz plane.
                    float halfPageDiag = block.getRealMax();
                    float pageMin = dist - halfPageDiag;
                    float pageMax = dist + halfPageDiag;
                    //Fading visibility check.
                    if (pageMax >= thisLvl.nearDist && pageMin < thisLvl.farTransDist) {
                        if (thisLvl.fadeEnabled && pageMax >= thisLvl.farDist) {
                            vis = true;
                            fadeEnable = true;
                            fadeStart = thisLvl.farDist;
                            fadeEnd = thisLvl.farTransDist;
                        } else if (nextLvl != null && nextLvl.fadeEnabled && pageMin < nextLvl.farTransDist) {
                            vis = true;
                            fadeEnable = true;
                            fadeStart = nextLvl.farTransDist;
                            fadeEnd = nextLvl.farDist;
                        }
                    }
                    block.setFade(fadeEnable, fadeStart, fadeEnd, l);
                } //If fade enabled

                block.setVisible(vis, l);
            }//Detail level loop
        }//Page loop
    }//Process method

    @Override
    public void unload() {
        if (blocks == null) {
            return;
        }
        //TODO Clean up better.
        if (future != null) {
            future.cancel(false);
        }
        if (blocks != null) {
            for (Block page : blocks) {
                if (page != null) {
                    page.unload();
                }
            }
        }
        blocks = null;
    }

    public float getBlockSize() {
        return blockSize;
    }

    public Vector3f getCenterPoint() {
        return centerPoint;
    }

    public PagingManager getManager() {
        return manager;
    }

    public short getPageSize() {
        return pageSize;
    }

    public short getResolution() {
        return resolution;
    }

    @Override
    public void increaseCacheTimer(float num) {
        cacheTimer = cacheTimer + num/bumps;
    }

    @Override
    public float getCacheTimer() {
        return cacheTimer;
    }

    @Override
    public void resetCacheTimer() {
        cacheTimer = 0;
        bumps++;
    }

    @Override
    public boolean isIdle() {
        return idle;
    }

    @Override
    public void setIdle(boolean idle) {
        this.idle = idle;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    @Override
    public boolean isPending() {
        return pending;
    }

    @Override
    public void setPending(boolean pending) {
        this.pending = pending;
    }

    @Override
    public Future<Boolean> getFuture() {
        return future;
    }

    @Override
    public void setFuture(Future<Boolean> future) {
        this.future = future;
    }

    @Override
    public Block createBlock(int x, int z, Vector3f center, PagingManager manager) {
        return new GeometryBlock(x,z,center,manager);
    }
}
