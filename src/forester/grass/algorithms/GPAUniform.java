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
package forester.grass.algorithms;

import com.jme3.math.FastMath;
import forester.RectBounds;
import forester.grass.GrassLayer;
import forester.grass.GrassBlock;
import forester.grass.GrassPage;
import forester.random.FastRandom;
import forester.image.DensityMap;
import forester.image.DensityMap.Channel;

/**
 * The default planting algorithm.
 * 
 * @author Andreas
 */
public class GPAUniform implements GrassPlantingAlgorithm {

    public enum Scaling {Linear, Quadratic, Linear_Inverted, Quadratic_Inverted}
    protected Scaling scaling = Scaling.Linear;
    
    protected float threshold = 0;
    protected boolean binary = false;
    
    public GPAUniform(){}
    
    public GPAUniform(float threshold){
        this.threshold = threshold;
    }
    
    @Override
    public int generateGrassData(   GrassPage page,
                                    GrassBlock block,
                                    GrassLayer layer,
                                    DensityMap densityMap,
                                    float[] grassData, 
                                    int grassCount
                                ) 
    {
        RectBounds bounds = block.getBounds();
        //Populating the array of locations (and also getting the total amount
        //of quads).
        FastRandom rand = new FastRandom();
        
        float width = bounds.getWidth();
        
        Channel channel = layer.getDmChannel();
        
        float offsetX = bounds.getxMin() - page.getCenterPoint().x + page.getPageSize()*0.5f;
        float offsetZ = bounds.getzMin() - page.getCenterPoint().z + page.getPageSize()*0.5f;
        
        //Iterator
        int iIt = 0;

        for (int i = 0; i < grassCount; i++) {
            
            float x = rand.unitRandom() * width;
            float z = rand.unitRandom() * width;
            float xx = x + offsetX;
            float zz = z + offsetZ;
            
            float d = densityMap.getDensityUnfiltered(xx, zz, channel);
            
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
            
            if (rand.unitRandom() + threshold < d ) {
                grassData[iIt++] = x + bounds.getxMin();
                grassData[iIt++] = z + bounds.getzMin();
                grassData[iIt++] = rand.unitRandom();
                // (-pi/2, pi/2]
                grassData[iIt++] = (-0.5f + rand.unitRandom())*3.141593f;
            }
        }
        //The iterator divided by four is the grass-count.
        return iIt/4;
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
    
}//GPAUniform
