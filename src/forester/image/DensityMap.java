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
package forester.image;

import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import java.nio.ByteBuffer;

/**
 * Class used for reading jME images.
 * 
 * @author Andreas
 */
public class DensityMap {
    
    //Enum of various different channels.
    public enum Channel {Red,Green,Blue,Alpha}
    
    protected Image image;
    protected int imageSize;
    protected ByteBuffer buf;
    
    protected float scale;
    protected int pageSize;
    
    boolean flipX = false, flipZ = true;
    
    public DensityMap(){}
    
    /**
     * Create a density map based on a texture.
     * 
     * @param tex The texture.
     * @param pageSize the pagesize used by the pageloader.
     */
    public DensityMap(Texture tex, int pageSize) {
        this.setupImage(tex.getImage());
        this.pageSize = pageSize;
        this.scale = imageSize/(float)pageSize;
    }
    
    protected final void setupImage(Image image){
        this.image = image;
        this.imageSize = image.getWidth();
        this.buf = image.getData(0).duplicate();
    }
    
    public void flipX(boolean flipX){
        this.flipX = flipX;
    }
    
    public void flipZ(boolean flipZ){
        this.flipZ = flipZ;
    }

    /**
     * A method to get density values from a densitymap.
     * 
     * @param x The x-coordinate.
     * @param z The z-coordinate.
     * @param channel The colorchannel to sample from.
     * @return The density value.
     */
    public float getDensityUnfiltered(float x, float z, Channel channel) {
        //Flip
        if(flipZ){
            z = pageSize - z;
        }
        if(flipX){
            x = pageSize - x;
        }
        int xx = max((int)(x*scale),0);
        int zz = min((int)(z*scale),imageSize - 1);
        return getValue(xx,zz,channel);
    }
    
    /**
     * Get the density using bilinear filtering.
     * 
     * @param x The x-coordinate.
     * @param z The z-coordinate.
     * @param channel The colorchannel to sample from.
     * @return The density value.
     */
    public float getDensityBilinear(float x, float z, Channel channel){
        //Flip
        if(flipZ){
            z = pageSize - z;
        }
        if(flipX){
            x = pageSize - x;
        }
        
        float fracX = x - (int)x;
        float fracZ = z - (int)z;
        
        int x0 = 0;
        int x1 = 0;
        int z0 = 0;
        int z1 = 0;
        
        if (fracX < 0.01){
            x0 = x1 = clamp((int)(x*scale),0,imageSize - 1);
        } else {
            x0 = max((int)(x*scale),0);
            x1 = min((int)((x + 1)*scale),imageSize - 1);
        }
        if (fracZ < 0.01){
            z0 = z1 = clamp((int)(z*scale),0,imageSize - 1);
        } else {
            z0 = max((int)(z*scale),0);
            z1 = min((int)((z + 1)*scale),pageSize - 1);
        }
        
        float v00 = getValue(x0,z0,channel);
        float v01 = getValue(x0,z1,channel);
        float v10 = getValue(x1,z0,channel);
        float v11 = getValue(x1,z1,channel);
        
        float dens = ( v00 * (1 - fracX) + v10 * fracX ) * (1 - fracZ) + 
                        (v01 * (1 - fracX) + v11 * fracX ) * fracZ;
        
        return dens;
    }
    
    //Get values from the image. This method assumes the x and y
    //vales are correct, and only deals with channels and pixel formats.
    protected float getValue(int x, int y, Channel channel){
        int position = x + imageSize*y;
        float f = 0;
        switch (image.getFormat()){
            case RGBA8:
                //Choose channel.
                switch(channel){
                    case Red:
                        buf.position(position*4);
                        f = byte2float(buf.get());
                    break;
                    case Green:
                        buf.position(position*4 + 1);
                        f = byte2float(buf.get());
                    break;
                    case Blue:
                        buf.position(position*4 + 2);
                        f = byte2float(buf.get());
                    break;
                    case Alpha:
                        buf.position(position*4 + 3);
                        f = byte2float(buf.get());
                    break;
                    default:
                        throw new UnsupportedOperationException("Image does not contain this channel.");
                }
                return f;
            case ABGR8:
                //Choose channel
                switch(channel){
                    case Red:
                        buf.position(position*4 + 3);
                        f = byte2float(buf.get());
                    break;
                    case Green:
                        buf.position(position*4 + 2);
                        f = byte2float(buf.get());
                    break;
                    case Blue:
                        buf.position(position*4 + 1);
                        f = byte2float(buf.get());
                    break;
                    case Alpha:
                        buf.position(position*4);
                        f = byte2float(buf.get());
                    break;
                    default:
                        throw new UnsupportedOperationException("Image does not contain this channel.");
                }
                return f;
            case RGB8:
                //Choose channel.
                switch(channel){
                    case Red:
                        buf.position(position*3);
                        f = byte2float(buf.get());
                    break;
                    case Green:
                        buf.position(position*3 + 1);
                        f = byte2float(buf.get());
                    break;
                    case Blue:
                        buf.position(position*3 + 2);
                        f = byte2float(buf.get());
                    break;
                    case Alpha:
                        f = 1;
                    break;
                    default:
                        throw new UnsupportedOperationException("Image does not contain this channel.");
                }
                return f;
            case BGR8:
                //Choose channel
                switch(channel){
                    case Red:
                        buf.position(position*3 + 2);
                        f = byte2float(buf.get());
                    break;
                    case Green:
                        buf.position(position*3 + 1);
                        f = byte2float(buf.get());
                    break;
                    case Blue:
                        buf.position(position*3);
                        f = byte2float(buf.get());
                    break;
                    case Alpha:
                        f = 1;
                    break;
                    default:
                        throw new UnsupportedOperationException("Image does not contain this channel.");
                }
                return f;
            default:
                throw new UnsupportedOperationException("Image format: "+image.getFormat());
        }
    }
    
    protected static float byte2float(byte b){
        return ((float)(b & 0xFF)) * 0.0039215f;
    }
    
    protected static int clamp(int x, int min, int max){
        int xx = (x >= min) ? x : min;
        return (xx <= max) ? xx : max;
    }
    
    protected static int max(int x, int val){
        return (x >= val) ? x : val;
    }
    
    protected static int min(int x, int val){
        return (x <= val) ? x : val;
    }
}
