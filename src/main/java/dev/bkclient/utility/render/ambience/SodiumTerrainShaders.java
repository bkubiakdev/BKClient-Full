package dev.bkclient.utility.render.ambience;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Extend Sodium's original shaders, retaining its atlas, lightmap, material and fog calculations. */
public final class SodiumTerrainShaders {
    private SodiumTerrainShaders() {}
    public static String patch(String namespace, String path, String source) {
        if (!namespace.equals("sodium")) return source;
        if (path.equals("blocks/block_layer_opaque.vsh")) {
            String anchor="vec3 position = _vert_position + translation;";
            if (!source.contains(anchor)) throw new IllegalStateException("Unsupported Sodium terrain vertex shader");
            source=source.replace("void main() {", resource("terrain_vertex.glsl")+"\nvoid main() {");
            return source.replace(anchor, anchor+"\n    bkStarsPosition = position + bkStarsOrigin;\n    bkDistance = length(position);\n    position += bkWind(position, a_Position.x >> 30u, _vert_tex_light_coord.y);\n");
        }
        if (path.equals("blocks/block_layer_opaque.fsh")) {
            String anchor="fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);";
            if (!source.contains(anchor)) throw new IllegalStateException("Unsupported Sodium terrain fragment shader");
            source=source.replace("void main() {", resource("terrain_fragment.glsl")+"\nvoid main() {");
            return source.replace(anchor,"if (bkStarsParams.y > 0.0) diffuseColor.rgb += bkStarsTint * bkSurfaceStars() * bkStarsParams.y;\n    "+anchor);
        }
        return source;
    }
    private static String resource(String name) {
        try (var stream=SodiumTerrainShaders.class.getResourceAsStream("/assets/bkclient/shaders/sodium/"+name)) {
            if (stream==null) throw new IOException("Missing "+name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch(IOException e) { throw new IllegalStateException(e); }
    }
}
