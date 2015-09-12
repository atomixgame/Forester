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
import com.jme3.scene.Node;
import forester.RectBounds;

/**
 * This is the blocks interface. Blocks could be seen as the leaves of the paging 
 * tree. Normally, all scenegraph contents are stored in blocks, whereas information
 * related to the paging process is stored within the (higher-level) page objects.
 * 
 * @author Andreas
 */
public interface Block{
    
    /**
     * This method is called to update the block. It has to be implemented but
     * it doesn't have to actually do anything.
     * 
     * @param tpf The number of seconds passed since the last frame. 
     */
    public void update(float tpf);
    
    /**
     * This method is called when the block is removed.
     */
    public void unload();
    
    /**
     * Get the centerpoint of the block.
     * 
     * @return The centerpoint.
     */
    public Vector3f getCenterPoint();
    
    public RectBounds getBounds();
    
    /**
     * Sets the nodes of the geometry block.
     * 
     * @param nodes The nodes to be set. 
     */
    public void setNodes(Node[] nodes);

    public Node[] getNodes();
    
    /**
     * Gets the node corresponding to the given level of detail.
     * 
     * @param detailLevel The level of detail.
     * @return The node.
     */
    public Node getNode(int detailLevel);
    
    /**
     * Gets the visibility status of a node.
     * 
     * @param detailLevel Detail levels are also used to index the nodes.
     * @return The visibility status of the node.
     */
    public boolean isVisible(int detailLevel);
    
    /**
     * Changes the visibility status of a node. The node index is determined by
     * the detailLevel parameter.
     * 
     * @param visible true or false.
     * @param detailLevel The level of detail.
     */
    public void setVisible(boolean visible, int detailLevel);
    
    /**
     * This mehod changes the fading status of a node.
     * 
     * @param enabled Whether or not fading is enabled.
     * @param fadeStart The distance where fading starts.
     * @param fadeEnd The distance where fading ends.
     * @param detailLevel The detailLevel (index) of the affected node.
     */
    public void setFade(boolean enabled, float fadeStart, float fadeEnd, int detailLevel);
    
    public float getRealMax();
    /**
     * This method is used to find out how large the geometry of the
     * block actually is (in the xz-plane). The real maximum value
     * is the radius of the smallest possible circle extending from the
     * block center, that covers all of the geometry inside the block.
     * 
     * This value defaults to blockSize / sqrt(2), which is the
     * distance from the block center to either of its corners.
     * 
     * @param detailLevel The level of detail (node index).
     */
    public void calculateRealMax(int detailLevel);
    
}//Page
