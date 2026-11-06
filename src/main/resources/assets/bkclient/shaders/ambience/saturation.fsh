#version 150

in vec2 TexCoord;
out vec4 OutColor;
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform mat4 InvViewProj;
uniform vec3 CenterRelative;
uniform int Radial;
uniform int Spherical;
uniform int IncludeSky;
uniform float GlobalSaturation;
uniform vec2 NearFar;
uniform vec2 RadiusTransition;

void main() {
    vec4 original = texture(Sampler0, TexCoord);
    float depth = texture(Sampler1, TexCoord).r;
    bool sky = depth >= 1.0;
    if (sky && IncludeSky == 0) {
        OutColor = original;
        return;
    }
    float saturation = GlobalSaturation;
    if (Radial != 0) {
        // Sky has no finite world position and receives the outside value.
        saturation = NearFar.y;
        if (!sky) {
            vec4 point = InvViewProj * vec4(TexCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
            if (abs(point.w) > 0.000001) {
                vec3 offset = point.xyz / point.w - CenterRelative;
                float distance = Spherical != 0 ? length(offset) : length(offset.xz);
                float radius = max(0.0, RadiusTransition.x);
                float blend = smoothstep(radius, radius + max(0.001, RadiusTransition.y), distance);
                saturation = mix(NearFar.x, NearFar.y, blend);
            }
        }
    }
    float luminance = dot(original.rgb, vec3(0.2126, 0.7152, 0.0722));
    OutColor = vec4(clamp(mix(vec3(luminance), original.rgb, clamp(saturation, 0.0, 2.0)), 0.0, 1.0), original.a);
}
