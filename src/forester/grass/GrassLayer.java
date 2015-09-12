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

import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import forester.MaterialSP;
import forester.grass.algorithms.GPAUniform;
import forester.grass.algorithms.GrassPlantingAlgorithm;
import forester.image.DensityMap.Channel;

/**
 * The GrassLayer class contains data specific to each type of grass.
 * 
 * @author Andreas
 */
public class GrassLayer {
    
    public enum MeshType {  QUADS,      //One static quad per patch of grass.
                            CROSSQUADS, //Two crossed static quads per patch of grass.
                            BILLBOARDS  //One billboarded quad per patch of grass.
                         }
    
    protected GrassLoader grassLoader;
    protected final MaterialSP material;
    
    protected MeshType type;
    protected GrassPlantingAlgorithm pa;
    
    protected float densityMultiplier = 1f;
    
    //The individual grass-patches height and width range.
    protected float maxHeight = 1.2f, minHeight = 1f;
    protected float maxWidth = 1.2f, minWidth = 1f;
    //This value is stored as the tangent of the slope, to save calculations.
    protected float maxTerrainSlope = (float) Math.tan(Math.toRadians(30.0));
    
    //Material parameters.
    protected boolean swaying = false;
    protected Vector3f swayData;
    protected Vector2f wind;
    protected boolean vertexLighting = true;
    protected boolean vertexColors;
    protected boolean selfShadowing;
    protected Texture colorMap;
    protected Texture alphaNoiseMap;
    
    protected ShadowMode shadowMode = ShadowMode.Off;
    
    //Related to density maps.
    protected Channel dmChannel = Channel.Red;
    protected int dmTexNum = 0;
    
    protected int cmTexNum = 0;
    
    /**
     * Don't use this constructor. Create new instances of this class only 
     * through the GrassLoaders addLayer-method.
     * 
     * @param mat The material for the grass.
     * @param type The type of mesh to use.
     * @param grassLoader The grassloader used to instantiate the layer.
     */
    protected GrassLayer(Material mat, MeshType type, GrassLoader grassLoader)
    {
        if(mat.getMaterialDef().getName().equals("BillboardGrass") || mat.getMaterialDef().getName().equals("Grass")){
            material = new MaterialSP(mat);
        } else {
            material = null;
            throw new RuntimeException("Material base not supported");
        }
        this.type = type;
        this.grassLoader = grassLoader;
        pa = new GPAUniform();
        initMaterial(mat);
    }
    
    /**
     * Internal method.
     */
    protected final void initMaterial(Material mat){
        
        this.colorMap = mat.getTextureParam("ColorMap").getTextureValue();
        this.alphaNoiseMap = mat.getTextureParam("AlphaNoiseMap").getTextureValue();
        
        if(material.getParam("VertexLighting") == null){
            this.vertexLighting = false;
        }
        
        if(mat.getParam("Swaying") == null){
            this.swaying = false;
        } else {
            this.swaying = (Boolean)material.getParam("Swaying").getValue();
        }
        
        if(mat.getParam("SwayData") == null){
            swayData = new Vector3f(1.0f,0.5f,300f);
        } else {
            this.swayData = (Vector3f)material.getParam("SwayData").getValue();
        }
        
        if(mat.getParam("Wind") == null){
            wind = new Vector2f(0,0);
        } else {
            this.wind = (Vector2f)material.getParam("Wind").getValue();
        }
    }
    
    protected void updateMaterial(){
        
        material.setTextureParam("AlphaNoiseMap",VarType.Texture2D,alphaNoiseMap);
        material.setTextureParam("ColorMap",VarType.Texture2D,colorMap);
        material.setBoolean("VertexLighting",vertexLighting);
        material.setBoolean("Swaying",swaying);
        material.setVector3("SwayData",swayData);
        material.setVector2("Wind", wind);
    }
    
    public void update(){
    }
    
    public void setMeshType(MeshType type){
        this.type = type;
    }
    
    public MeshType getMeshType(){
        return type;
    }

    public float getDensityMultiplier() {
        return densityMultiplier;
    }

    public void setDensityMultiplier(float density) {
        FastMath.clamp(density, 0, 1);
        densityMultiplier = density;
    }

    public float getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(float maxHeight) {
        if(maxHeight < minHeight){
            throw new RuntimeException("Max height needs to be larger then or equal to min height.");
        }
        this.maxHeight = maxHeight;
    }

    public float getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(float maxWidth) {
        if(maxWidth < minWidth){
            throw new RuntimeException("Max width needs to be larger then or equal to min width.");
        }
        this.maxWidth = maxWidth;
    }

    public float getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(float minHeight) {
        if(minHeight > maxHeight){
            throw new RuntimeException("Min height needs to be smaller then or equal to max height.");
        }
        this.minHeight = minHeight;
    }

    public float getMinWidth() {
        return minWidth;
    }

    public void setMinWidth(float minWidth) {
        if(minWidth > maxWidth){
            throw new RuntimeException("Min width needs to be smaller then or equal to max width.");
        }
        this.minWidth = minWidth;
    }
    
    /**
     * Set the maximum slope of the terrain in degrees. If the terrain
     * slope is higher then that number, no grass will be planted there.
     * 
     * @param angleInDegrees An angle between 0 and 90 (degrees).
     */
    public void setMaxTerrainSlope(float angleInDegrees){
        float aID = FastMath.clamp(angleInDegrees, 0, 90);
        maxTerrainSlope = (float) Math.tan(Math.toRadians(aID));
    }
    
    public float getMaxTerrainSlope(){
        return maxTerrainSlope;
    }
    
    public boolean isSwaying(){
        return swaying;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public void setSwaying(boolean swaying) {
        this.swaying = swaying;
        material.setBoolean("Swaying", swaying);
    }
    
    public void setWind(Vector2f wind) {
        this.wind = wind;
        material.setVector2("Wind", wind);
    }

    public void setSwayData(Vector3f swayData) {
        this.swayData = swayData;
        material.setVector3("SwayData", swayData);
    }
    
    public void setSwayingFrequency(float distance){
        swayData.x = distance;
        material.setVector3("SwayData", swayData);
    }
    
    public void setSwayingVariation(float distance){
        swayData.y = distance;
        material.setVector3("SwayData", swayData);
    }

    public Texture getAlphaNoiseMap() {
        return alphaNoiseMap;
    }

    public void setAlphaNoiseMap(Texture alphaNoiseMap) {
        this.alphaNoiseMap = alphaNoiseMap;
        material.setTexture("AlphaNoiseMap", alphaNoiseMap);
    }

    public Texture getColorMap() {
        return colorMap;
    }

    public void setColorMap(Texture colorMap) {
        this.colorMap = colorMap;
        material.setTexture("ColorMap", colorMap);
    }

    public GrassLoader getGrassLoader() {
        return grassLoader;
    }

    public void setGrassLoader(GrassLoader grassLoader) {
        this.grassLoader = grassLoader;
    }

    public boolean isVertexLighting() {
        return vertexLighting;
    }

    public void setVertexLighting(boolean vertexLighting) {
        this.vertexLighting = vertexLighting;
        material.setBoolean("VertexLighting", vertexLighting);
    }
    
    public GrassPlantingAlgorithm getPlantingAlgorithm() {
        return pa;
    }

    public void setPlantingAlgorithm(GrassPlantingAlgorithm plantingAlgorithm) {
        this.pa = plantingAlgorithm;
    }

    public Channel getDmChannel() {
        return dmChannel;
    }
    
    public int getDmTexNum(){
        return dmTexNum;
    }
    
    public void setDensityTextureData(int dmTexNum, Channel channel){
        this.dmChannel = channel;
        this.dmTexNum = dmTexNum;
    }

    public int getCmTexNum() {
        return cmTexNum;
    }
    
    public void setVertexColorMapData(int cmTexNum){
        this.cmTexNum = cmTexNum;
    }

    public ShadowMode getShadowMode() {
        return shadowMode;
    }

    public void setShadowMode(ShadowMode shadowMode) {
        this.shadowMode = shadowMode;
    }
    
}//GrassLayer
