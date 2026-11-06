#version 150

out vec2 TexCoord;

void main() {
    vec2 vertices[6] = vec2[](
        vec2(-1, -1), vec2(1, -1), vec2(1, 1),
        vec2(-1, -1), vec2(1, 1), vec2(-1, 1)
    );
    vec2 p = vertices[gl_VertexID];
    TexCoord = p * 0.5 + 0.5;
    gl_Position = vec4(p, 0, 1);
}
