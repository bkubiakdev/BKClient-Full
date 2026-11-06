package dev.bkclient.ui.clicksys.port.concurrent;
import dev.bkclient.ui.clicksys.port.render.ShaderAsset;
import java.nio.charset.StandardCharsets;
public final class ShaderPackStore {
 public static void update2(){}
 public static byte[] getByteArrayByString(String name){return resource(name+".png");}
 public static ShaderAsset getShaderAssetByString(String name){byte[] data=resource(name+".json");if(data==null)return null;
 var j=new com.google.gson.JsonObject();j.addProperty("name",name);j.addProperty("jsonData",new String(data,StandardCharsets.UTF_8));return new com.google.gson.Gson().fromJson(j,ShaderAsset.class);}
 private static byte[] resource(String name){try(var in=ShaderPackStore.class.getResourceAsStream("/assets/bkclicksys/fonts/"+name)){return in==null?null:in.readAllBytes();}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
}