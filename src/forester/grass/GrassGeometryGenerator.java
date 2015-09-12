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
package forester.grass;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.TerrainQuad;
import forester.RectBounds;
import forester.grass.GrassLayer.MeshType;
import forester.image.DensityMap;
import java.nio.Buffer;

/**
 * This class contains a few methods for generating grass meshes.
 * 
 * @author Andreas
 */
public class GrassGeometryGenerator {
    
    protected Terrain terrain;
    protected float terrainHeight;
    protected Vector2f hStore = new Vector2f();
    
    public GrassGeometryGenerator(Terrain terrain){
        this.terrain = terrain;
        terrainHeight = ((TerrainQuad)terrain).getLocalTranslation().getY();
    }
    
    /**
     * This method creates a grass geometry. This is this method you call
     * from the grassloader.
     * 
     * @param layer The grasslayer.
     * @param block The grassblock.
     * @param page The grass page.
     * @param densityMap The densitymap (or null).
     * @return A batched grass geometry.
     */
    public Geometry createGrassGeometry(GrassLayer layer,
                                        GrassBlock block,
                                        GrassPage page,
                                        DensityMap densityMap
                                        )
    {
        RectBounds bounds = block.getBounds();
        //Calculate the area of the page
        float area = bounds.getWidth()*bounds.getWidth();
            
        //This is the grasscount variable. The initial value is the maximum
        //possible count. It may be reduced by densitymaps, height restrictions
        //and other stuff.
        int grassCount = (int) (area * layer.getDensityMultiplier());
        
        //Each "grass data point" consists of coords (x,z), scale and rotation-angle.
        //That makes 4 data points per patch of grass.
        float[] grassData = new float[grassCount*4];
        
        //The planting algorithm returns the final amount of grass.
        grassCount = layer.getPlantingAlgorithm().generateGrassData(page, block, layer, densityMap, grassData, grassCount);
        
        Mesh grassMesh = new Mesh();
        
        MeshType meshType = layer.getMeshType();
        
        //No need running this if there's no grass data.
        if(grassCount != 0)
        {
            if(meshType == MeshType.QUADS){
                grassMesh = generateGrass_QUADS(layer,block,grassData,grassCount);
            } else if(meshType == MeshType.CROSSQUADS){
                grassMesh = generateGrass_CROSSQUADS(layer,block,grassData,grassCount);
            } else if(meshType == MeshType.BILLBOARDS){
                grassMesh = generateGrass_BILLBOARDS(layer,block,grassData,grassCount);
            }
        }
        
        grassMesh.setStatic();
        grassMesh.updateCounts();
        Geometry geom = new Geometry();
        geom.setMesh(grassMesh);
        geom.setMaterial(layer.getMaterial().clone());
        geom.setQueueBucket(Bucket.Transparent);
        
        return geom;
    }
    
    /**
     * Method for creating a static quad mesh.
     *
     * @param layer The grass-layer.
     * @param block The grassblock.
     * @param grassData The grassdata array. See the createGrassGeometry method.
     * @param grassCount The initial grass-count. See the createGrassGeometry method.
     * 
     * @return A static quad mesh.
     */
    protected Mesh generateGrass_QUADS( GrassLayer layer,
                                        GrassBlock block,
                                        float[] grassData, 
                                        int grassCount
                                      )
    {
        //The grass mesh
        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Triangles);
        
        // ***************** Setting up the mesh buffers. *****************
        
        //Each grass has four positions, each vertice is 3 floats
        float[] positions = new float[grassCount*12];
        float[] normals = new float[grassCount*12];
        //Each grass has got 4 texture coordinates, each coord is 2 floats.
        float[] texCoords = new float[grassCount*8];
        
        //Slim the mesh down a little.
        Format form = Format.UnsignedShort;
        if (grassCount*4 > 65535) {
            form = Format.UnsignedInt;
        } else if (grassCount*4 > 255){ 
            form = Format.UnsignedShort;
        } else {
            form = Format.UnsignedByte;
        }
        
        Buffer data = VertexBuffer.createBuffer(form, 1, grassCount*6);           
        VertexBuffer iBuf = new VertexBuffer(VertexBuffer.Type.Index);
        iBuf.setupData(VertexBuffer.Usage.Dynamic, 1, form, data);
        mesh.setBuffer(iBuf);
        
        //Getting the dimensions
        float minHeight = layer.getMinHeight();
        float maxHeight = layer.getMaxHeight();
        
        float minWidth = layer.getMinWidth();
        float maxWidth = layer.getMaxWidth();
        
        //A bunch of array iterators.
        //Grass data iterator
        int gIt = 0;
        //position,texcoord, angle and color iterators
        int pIt = 0;
        int tIt = 0;
        int nIt = 0;
        
        RectBounds bounds = block.getBounds();
        float cX = bounds.getCenter().x;
        float cZ = bounds.getCenter().z;
        
        float maxSlope = layer.getMaxTerrainSlope();
        
        //Generating quads
        for(int i = 0; i < grassCount; i++)
        {
            //Position values
            float x = grassData[gIt++];
            float z = grassData[gIt++];
            float size = grassData[gIt++];
            float angle = grassData[gIt++];
            
            float halfScaleX = (minWidth + size*(maxWidth - minWidth))*0.5f;
            float scaleY = minHeight + size*(maxHeight - minHeight);
            
            float xAng = (float)(Math.cos(angle));
            float zAng = (float)(Math.sin(angle));
            
            float xTrans = xAng * halfScaleX;
            float zTrans = zAng * halfScaleX;
            
            float x1 = x - xTrans, z1 = z - zTrans;
            float x2 = x + xTrans, z2 = z + zTrans;
            
            float y1 = getTerrainHeight(x1,z1);
            float y2 = getTerrainHeight(x2,z2);
            
            // Check the angle between y1 and y2. If too steep, collapse the quad.
            float tanDYDX = FastMath.abs((y2 - y1)/(x2 - x1));
            
            float y1h = y1;
            float y2h = y2;
            
            float tC = 0;
            
            //If angle is within bounds, generate a proper quad.
            if(tanDYDX < maxSlope){
                y1h = y1 + scaleY;
                y2h = y2 + scaleY;
                tC = 1;
            }            
                        
            // ******************** Adding vertices ********************** 
            
            positions[pIt++] = x1 - cX;                         //pos
            positions[pIt++] = y1h;
            positions[pIt++] = z1 - cZ;
            
            normals[nIt++] = zAng;                              //normal
            normals[nIt++] = 0;
            normals[nIt++] = -xAng;
            texCoords[tIt++] = 0;    texCoords[tIt++]= tC;    //uv
            
            positions[pIt++] = x2 - cX;                         //pos
            positions[pIt++] = y2h;
            positions[pIt++] = z2 - cZ;
            
            normals[nIt++] = zAng;                              //normal
            normals[nIt++] = 0;
            normals[nIt++] = -xAng;
            texCoords[tIt++] = tC;   texCoords[tIt++]= tC;     //uv
            
            positions[pIt++] = x1 - cX;                         //pos
            positions[pIt++] = y1;
            positions[pIt++] = z1 - cZ;
            
            normals[nIt++] = zAng;                              //normal
            normals[nIt++] = 0;
            normals[nIt++] = -xAng;
            texCoords[tIt++] = 0;  texCoords[tIt++]=0;      //uv
            
            positions[pIt++] = x2 - cX;                         //pos
            positions[pIt++] = y2;
            positions[pIt++] = z2 - cZ;
            
            normals[nIt++] = zAng;                              //normal
            normals[nIt++] = 0;
            normals[nIt++] = -xAng;
            texCoords[tIt++] = tC;   texCoords[tIt++]=0;      //uv

        }
        
        //************************ Indices **************************
        
        int iIt = 0;
        
        int offset = 0;
        IndexBuffer iB = mesh.getIndexBuffer();
        for(int i = 0; i < grassCount; i++){
            offset = i*4;
            iB.put(iIt++, 0 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 1 + offset);
                
            iB.put(iIt++, 1 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 3 + offset);
        }
        
        
        // ******************** Finalizing the mesh ***********************
                
        // Setting buffers
        mesh.setBuffer(Type.Position, 3, positions);
        mesh.setBuffer(Type.TexCoord, 2, texCoords);
        mesh.setBuffer(Type.Normal,   2, normals);
        
        BoundingBox box = new BoundingBox();
                
        mesh.setBound(box);
        mesh.updateBound();
        return mesh;
    }
    
    /**
     * Method for creating a static cross-quad mesh.
     * 
     * @param layer The grass-layer.
     * @param block The grassblock.
     * @param grassData The grassdata array. See the createGrassGeometry method.
     * @param grassCount The initial grass-count. See the createGrassGeometry method.
     * @return A static cross-quad mesh.
     */
    protected Mesh generateGrass_CROSSQUADS(GrassLayer layer,
                                            GrassBlock block,
                                            float[] grassData, 
                                            int grassCount
                                            )
    {
        //The grass mesh
        Mesh mesh = new Mesh();
        
        mesh.setMode(Mesh.Mode.Triangles);        
        
        // ***************** Setting up the mesh buffers. *****************
        
        //Each grass has eight positions, each position is 3 floats.
        float[] positions = new float[grassCount*24];
        //Each grass has got eight texture coordinates, each coord is 2 floats.
        float[] texCoords = new float[grassCount*16];
        //This is the angle of the quad.
        float[] normals = new float[grassCount*24];
        
        //Slim the mesh down a little.
        Format form = Format.UnsignedShort;
        if (grassCount*4 > 65535) {
            form = Format.UnsignedInt;
        } else if (grassCount*4 > 255){ 
            form = Format.UnsignedShort;
        } else {
            form = Format.UnsignedByte;
        }
        
        Buffer data = VertexBuffer.createBuffer(form, 1, grassCount*12);           
        VertexBuffer iBuf = new VertexBuffer(VertexBuffer.Type.Index);
        iBuf.setupData(VertexBuffer.Usage.Dynamic, 1, form, data);
        mesh.setBuffer(iBuf);
        
        //Getting the dimensions
        float minHeight = layer.getMinHeight();
        float maxHeight = layer.getMaxHeight();
        
        float minWidth = layer.getMinWidth();
        float maxWidth = layer.getMaxWidth();
        
        //A bunch of array iterators.
        //Grass data iterator
        int gIt = 0;
        //position, texcoord and angle iterators
        int pIt = 0;
        int tIt = 0;
        int nIt = 0;
        
        RectBounds bounds = block.getBounds();
        float cX = bounds.getCenter().x;
        float cZ = bounds.getCenter().z;
        
        float maxSlope = layer.getMaxTerrainSlope();
        //Generating quads
        for(int i = 0; i < grassCount; i++)
        {
            //Position values
            float x = grassData[gIt++];
            float z = grassData[gIt++];
            float size = grassData[gIt++];
            float angle = grassData[gIt++];
            
            float halfScaleX = (minWidth + size*(maxWidth - minWidth))*0.5f;
            float scaleY = minHeight + size*(maxHeight - minHeight);
            
            float xAng = (float)(Math.cos(angle));
            float zAng = (float)(Math.sin(angle));
            
            float xTrans = xAng * halfScaleX;
            float zTrans = zAng * halfScaleX;
            
            float x1 = x - xTrans, z1 = z - zTrans;
            float x2 = x + xTrans, z2 = z + zTrans;
            float x3 = x + zTrans, z3 = z - xTrans;
            float x4 = x - zTrans, z4 = z + xTrans;
            
            float y1 = getTerrainHeight(x1,z1); 
            float y2 = getTerrainHeight(x2,z2);
            float y3 = getTerrainHeight(x3,z3);
            float y4 = getTerrainHeight(x4,z4);
            
            // Check the angles. If too steep, collapse the quad.
            float tanDYDX1 = FastMath.abs((y2 - y1)/(x2 - x1));
            float tanDYDX2 = FastMath.abs((y4 - y3)/(x4 - x3));
            
            float y1h = y1;
            float y2h = y2;
            float y3h = y3;
            float y4h = y4;
            
            float tC = 0;
            
            //If angles are within bounds, generate a crossquad.
            if( tanDYDX1 < maxSlope && tanDYDX2 < maxSlope ){
                y1h = y1 + scaleY;
                y2h = y2 + scaleY;
                y3h = y3 + scaleY;
                y4h = y4 + scaleY;
                tC = 1;
            }
            
            
            
            
            
            //************Generate the first quad**************
            
            positions[pIt++] = x1 - cX;                         //pos
            positions[pIt++] = y1h;
            positions[pIt++] = z1 - cZ;
            
            normals[nIt++] = zAng;                              //normal
            normals[nIt++] = 0;
            normals[nIt++] = -xAng;
            
            texCoords[tIt++] = 0;    texCoords[tIt++] = tC;     //uv
            
            positions[pIt++] = x2 - cX;                         //pos
            positions[pIt++] = y2h;
            positions[pIt++] = z2 - cZ;
            
            normals[nIt++] = zAng;                              //normal
            normals[nIt++] = 0;
            normals[nIt++] = -xAng;
            
            texCoords[tIt++] = tC;   texCoords[tIt++] = tC;     //uv
            
            positions[pIt++] = x1 - cX;                         //pos
            positions[pIt++] = y1; 
            positions[pIt++] = z1 - cZ; 
            
            normals[nIt++] = zAng;                              //normal
            normals[nIt++] = 0;
            normals[nIt++] = -xAng;
            
            texCoords[tIt++] = 0;  texCoords[tIt++] = 0;        //uv
            
            positions[pIt++] = x2 - cX;                         //pos
            positions[pIt++] = y2;
            positions[pIt++] = z2 - cZ;
            
            normals[nIt++] = zAng;                              //normal
            normals[nIt++] = 0;
            normals[nIt++] = -xAng;
            
            texCoords[tIt++] = tC;  texCoords[tIt++] = 0;       //uv
            
            //************Generate the second quad**************
            
            positions[pIt++] = x3 - cX;                         //pos
            positions[pIt++] = y3h;
            positions[pIt++] = z3 - cZ;
            
            normals[nIt++] = xAng;                              //normal
            normals[nIt++] = 0; 
            normals[nIt++] = zAng;
            
            texCoords[tIt++] = 0;    texCoords[tIt++] = tC;     //uv
            
            positions[pIt++] = x4 - cX;                         //pos
            positions[pIt++] = y4h;
            positions[pIt++] = z4 - cZ;
            
            normals[nIt++] = xAng;                              //normal
            normals[nIt++] = 0; 
            normals[nIt++] = zAng;
            
            texCoords[tIt++] = tC;   texCoords[tIt++]= tC;      //uv
            
            positions[pIt++] = x3 - cX;                         //pos
            positions[pIt++] = y3; 
            positions[pIt++] = z3 - cZ;
            
            normals[nIt++] = xAng;                              //normal
            normals[nIt++] = 0; 
            normals[nIt++] = zAng;
            
            texCoords[tIt++] = 0;  texCoords[tIt++]= 0;         //uv
            
            positions[pIt++] = x4 - cX;                         //pos
            positions[pIt++] = y4;
            positions[pIt++] = z4 - cZ;
            
            normals[nIt++] = xAng;                              //normal
            normals[nIt++] = 0; 
            normals[nIt++] = zAng;
            
            texCoords[tIt++] = tC;  texCoords[tIt++]= 0;        //uv

        }
        
        //Indices
        int iIt = 0;
        
        int offset = 0;
        IndexBuffer iB = mesh.getIndexBuffer();
        for(int i = 0; i < grassCount; i++){
            offset = i*8;
            iB.put(iIt++, 0 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 1 + offset);
                
            iB.put(iIt++, 1 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 3 + offset);
            
            iB.put(iIt++, 4 + offset);
            iB.put(iIt++, 6 + offset);
            iB.put(iIt++, 5 + offset);
                
            iB.put(iIt++, 5 + offset);
            iB.put(iIt++, 6 + offset);
            iB.put(iIt++, 7 + offset);
        }
        
        //********************* Finalizing the mesh ***********************
        
        // Setting buffers
        mesh.setBuffer(Type.Position, 3, positions);
        mesh.setBuffer(Type.TexCoord, 2, texCoords);
        mesh.setBuffer(Type.Normal,   2, normals);
        
        BoundingBox box = new BoundingBox();
        
        mesh.setBound(box);
        mesh.updateBound();
        return mesh;
    }
    
    /**
     * Method for creating a billboarded quad mesh. Billboarded quad meshes
     * requires a certain type of shader to work.
     * 
     * @param layer The grasslayer.
     * @param block The grassblock.
     * @param grassData The grassdata array. See the createGrassGeometry method.
     * @param grassCount The initial grass-count. See the createGrassGeometry method.
     * @return A billboarded quad mesh.
     */
    protected Mesh generateGrass_BILLBOARDS(    GrassLayer layer,
                                                GrassBlock block,
                                                float[] grassData, 
                                                int grassCount
                                           )
    {
        Mesh mesh = new Mesh();
        
        mesh.setMode(Mesh.Mode.Triangles);        
        
        // ***************** Setting up the mesh buffers. *****************
        
        //Each grass has four positions, each vertice is 3 floats
        float[] positions = new float[grassCount*12];
        //Each grass has got 4 texture coordinates, each coord is 2 floats.
        float[] texCoords = new float[grassCount*8];
        //Each vertex need a texCoord for displacement data.
        float[] texCoords2 = new float[grassCount*8];
                
        //Slim the mesh down a little.
        Format form = Format.UnsignedShort;
        if (grassCount*4 > 65535) {
            form = Format.UnsignedInt;
        } else if (grassCount*4 > 255){ 
            form = Format.UnsignedShort;
        } else {
            form = Format.UnsignedByte;
        }
        
        Buffer data = VertexBuffer.createBuffer(form, 1, grassCount*6);           
        VertexBuffer iBuf = new VertexBuffer(VertexBuffer.Type.Index);
        iBuf.setupData(VertexBuffer.Usage.Dynamic, 1, form, data);
        mesh.setBuffer(iBuf);
        
        //Getting the dimensions
        float minHeight = layer.getMinHeight();
        float maxHeight = layer.getMaxHeight();
        
        float minWidth = layer.getMinWidth();
        float maxWidth = layer.getMaxWidth();
        
        //A bunch of array iterators.
        //Grass data iterator
        int gIt = 0;
        //position, texcoord and color iterators
        int pIt = 0;
        int tIt = 0;
        int t2It = 0;
        
        
        RectBounds bounds = block.getBounds();
        float cX = bounds.getCenter().x;
        float cZ = bounds.getCenter().z;
    
        //Generating quads
        for(int i = 0; i < grassCount; i++)
        {
            //Position values
            float x = grassData[gIt++];
            float z = grassData[gIt++];
            float size = grassData[gIt++];
            //Not using angle.
            gIt++;
            
            float halfScaleX = (minWidth + size*(maxWidth - minWidth))*0.5f;
            float scaleY = minHeight + size*(maxHeight - minHeight);
            
            float y = getTerrainHeight(x,z);
            
            float xx = x - cX;
            float zz = z - cZ;
            // ******************** Adding vertices ********************** 
            
            positions[pIt++] = xx;                                              //pos
            positions[pIt++] = y;
            positions[pIt++] = zz;
            
            texCoords[tIt++] = 0.f;    texCoords[tIt++]=1.f;                    //uv
            texCoords2[t2It++] = -halfScaleX;   texCoords2[t2It++] = scaleY;    //disp
            
            positions[pIt++] = xx;                                              //pos
            positions[pIt++] = y;
            positions[pIt++] = zz;
            
            texCoords[tIt++] = 1.f;   texCoords[tIt++]=1.f;                     //uv
            texCoords2[t2It++] = halfScaleX;   texCoords2[t2It++] = scaleY;     //disp
            
            positions[pIt++] = xx;                                              //pos
            positions[pIt++] = y; 
            positions[pIt++] = zz; 
            
            
            texCoords[tIt++] = 0.f;  texCoords[tIt++]=0.f;                      //uv
            texCoords2[t2It++] = -halfScaleX;   texCoords2[t2It++] = 0.f;       //disp
            
            positions[pIt++] = xx;                                              //pos
            positions[pIt++] = y;
            positions[pIt++] = zz;
            
            
            texCoords[tIt++] = 1.f;  texCoords[tIt++]=0.f;                      //uv
            texCoords2[t2It++] = halfScaleX;   texCoords2[t2It++] = 0.f;        //disp

        }
        
        //Indices
        int iIt = 0;
        
        int offset = 0;
        IndexBuffer iB = mesh.getIndexBuffer();
        for(int i = 0; i < grassCount; i++){
            offset = i*4;
            iB.put(iIt++, 0 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 1 + offset);
                
            iB.put(iIt++, 1 + offset);
            iB.put(iIt++, 2 + offset);
            iB.put(iIt++, 3 + offset);
        }
        
        // ******************** Finalizing the mesh ***********************
                
        // Setting buffers
        mesh.setBuffer(Type.Position, 3, positions);
        mesh.setBuffer(Type.TexCoord, 2, texCoords);
        mesh.setBuffer(Type.TexCoord2,2, texCoords2);
        BoundingBox box = new BoundingBox();
        
        mesh.setBound(box);
        mesh.updateBound();
        
        return mesh;
    }
    
    protected float getTerrainHeight(float x, float z){
        hStore.set(x, z);
        return terrain.getHeight(hStore) + terrainHeight;
    }
    
}//GrassGeometryGenerator
