#version 150

uniform sampler2D Snapshot;

in vec2 texCoord;
in float vertexAlpha;

out vec4 fragColor;

void main() {
    // The snapshot is a framebuffer texture, so its origin is bottom-left while the
    // shard UVs are top-left like the rest of the GUI.
    vec4 colour = texture(Snapshot, vec2(texCoord.x, 1.0 - texCoord.y));
    // Only the vertex alpha drives transparency. The snapshot is an opaque screen
    // grab and its alpha channel is whatever the main framebuffer happened to be
    // cleared to, so multiplying by it would erase every shard.
    fragColor = vec4(colour.rgb, vertexAlpha);
}
