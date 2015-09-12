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
package forester.trees;

import com.jme3.math.Vector3f;
import forester.paging.GeometryPage;
import forester.paging.interfaces.PagingManager;
import forester.trees.datagrids.TreeDataBlock;

/**
 * This is the default tile-type for the treeloader.
 * 
 * @author Andreas
 */
public class TreePage extends GeometryPage{
    
    protected TreeDataBlock block;

    public TreePage(int x, int z, PagingManager engine) {
        super(x, z, engine);
    }

    @Override
    public TreeBlock createBlock(int x, int y, Vector3f center, PagingManager engine) {
        return new TreeBlock(x,y,center,engine);
    }

    @Override
    public void unload() {
        super.unload();
        block = null;
    }

    public TreeDataBlock getBlock() {
        return block;
    }

    public void setBlock(TreeDataBlock block) {
        this.block = block;
    }
}
