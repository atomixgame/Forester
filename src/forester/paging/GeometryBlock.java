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

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import forester.RectBounds;
import forester.paging.grid.GenericCell2D;
import forester.paging.interfaces.Block;
import forester.paging.interfaces.PagingManager;

/**
 * Base class for pages.
 * 
 * @author Andreas
 */
public class GeometryBlock extends GenericCell2D implements Block {

    protected Node parentNode;
    protected Node[] nodes;
    protected boolean[] stateVec;
    protected float realMax;
    protected RectBounds bounds;

    /**
     * Constructor based on x and z coordinates.
     * 
     * @param x The x-coordinate of the page.
     * @param z The z-coordinate of the page.
     * @param center The center of the page.
     * @param engine The paging engine used with this page(type).
     */
    public GeometryBlock(int x, int z, Vector3f center, PagingManager engine) {
        super(x, z);
        this.parentNode = engine.getPagingNode();
        bounds = new RectBounds(center,engine.getBlockSize());
    }

    @Override
    public void setNodes(Node[] nodes) {
        this.nodes = nodes;
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].setLocalTranslation(bounds.getCenter());
        }
        stateVec = new boolean[nodes.length];
    }

    @Override
    public Node[] getNodes() {
        return nodes;
    }

    @Override
    public Node getNode(int detailLevel) {
        return nodes[detailLevel];
    }

    @Override
    public boolean isVisible(int detailLevel) {
        return stateVec[detailLevel];
    }

    @Override
    public void setVisible(boolean visible, int detailLevel) {
        if (visible == true && stateVec[detailLevel] == false) {
            parentNode.attachChild(nodes[detailLevel]);
            stateVec[detailLevel] = visible;
        } else if (visible == false && stateVec[detailLevel] == true) {
            nodes[detailLevel].removeFromParent();
            stateVec[detailLevel] = visible;
        }
    }

    @Override
    public void setFade(boolean enabled, float fadeStart, float fadeEnd, int detailLevel) {
        float fadeRange = fadeEnd - fadeStart;
        Material material = null;
        for (Spatial spat : nodes[detailLevel].getChildren()) {
            material = ((Geometry) spat).getMaterial();
            material.setFloat("FadeEnd", fadeEnd);
            material.setFloat("FadeRange", fadeRange);
            material.setBoolean("FadeEnabled", enabled);
        }
    }

    @Override
    public void unload() {
        if (nodes != null) {
            for (int i = 0; i < nodes.length; i++) {
                setVisible(false, i);
            }
            nodes = null;
            stateVec = null;
        }
    }

    @Override
    public Vector3f getCenterPoint() {
        return bounds.getCenter();
    }
    
    @Override
    public RectBounds getBounds(){
        return bounds;
    }

    @Override
    public float getRealMax() {
        return realMax;
    }

    @Override
    public void calculateRealMax(int detailLevel) {

        Node node = nodes[detailLevel];
        if(node.getWorldBound() == null){
            realMax = bounds.getWidth()*0.70711f; //Half the pagesize times sqrt(2).
            return;
        }
        node.updateGeometricState();
        
        float ol = bounds.getWidth()*0.70711f; // Half blocksize * sqrt(2).
        
        BoundingVolume wb = node.getWorldBound();
        // The difference between bounding volume and block centers.
        // These values needs to be added to the real max.
        float dX = FastMath.abs(wb.getCenter().x - bounds.getCenter().x);
        float dZ = FastMath.abs(wb.getCenter().z - bounds.getCenter().z);
        
        if (wb instanceof BoundingSphere) {
            BoundingSphere bs = (BoundingSphere) wb;
            float radius = bs.getRadius();
            
            float temp = dX + radius;
            if (temp > ol) {
                ol = temp;
            }
            
            temp = dZ + radius;
            if ( temp > ol) {
                ol = temp;
            }
            
        } else if (node.getWorldBound() instanceof BoundingBox){
            BoundingBox bb = (BoundingBox) wb;
            
            float temp = (dX + bb.getXExtent())*1.4142f; //sqrt(2)
            if (temp > ol) {
                ol = temp;
            }
            
            temp = (dZ + bb.getZExtent())*1.4142f;
            if (temp > ol) {
                ol = temp;
            }
        }

        realMax = ol;
    }

    @Override
    public void update(float tpf) {
    }
}
