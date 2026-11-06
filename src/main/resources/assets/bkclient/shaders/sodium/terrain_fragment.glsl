in vec3 bkStarsPosition;
in float bkDistance;
uniform vec4 bkStarsParams;
uniform vec3 bkStarsTint;
float starHash(vec3 p) {
    p = fract(p * vec3(443.8975, 397.2973, 491.1871));
    p += dot(p, p.yxz + 19.19);
    return fract((p.x + p.y) * p.z);
}
// Approximate the pixel's surface footprint, preserving each lobe's energy.
// Merely adding fwidth to the radius makes distant stars brighter and streaky.
float filteredStar(vec2 d, vec2 dx, vec2 dy, vec2 radius) {
    vec2 variance=radius*radius;
    float a=variance.x+(dx.x*dx.x+dy.x*dy.x)/12.0;
    float b=(dx.x*dx.y+dy.x*dy.y)/12.0;
    float c=variance.y+(dx.y*dx.y+dy.y*dy.y)/12.0;
    float determinant=max(a*c-b*b,1e-12);
    float exponent=(c*d.x*d.x-2.0*b*d.x*d.y+a*d.y*d.y)/determinant;
    return sqrt(variance.x*variance.y/determinant)*exp(-0.5*max(exponent,0.0));
}
float bkSurfaceStars() {
    vec3 n = abs(cross(dFdx(bkStarsPosition), dFdy(bkStarsPosition)));
    vec2 uv;
    float plane;
    if (n.y >= n.x && n.y >= n.z) { uv=bkStarsPosition.xz; plane=bkStarsPosition.y; }
    else if(n.x >= n.z) { uv=bkStarsPosition.zy; plane=bkStarsPosition.x; }
    else { uv=bkStarsPosition.xy; plane=bkStarsPosition.z; }
    uv *= bkStarsParams.w;
    // Derivatives must precede the per-cell divergent branch below.
    vec2 dx=dFdx(uv), dy=dFdy(uv);
    float lod=1.0-smoothstep(0.12,0.5,max(length(dx),length(dy)));
    vec2 cell=mod(floor(uv),256.0);
    float seed=starHash(vec3(cell, mod(floor(plane*16.0+0.01),4096.0)));
    if(seed < 0.45) return 0.0;
    vec2 center=vec2(starHash(vec3(cell,seed*37.0)),starHash(vec3(cell,seed*91.0)))*0.5+0.25;
    vec2 d=(fract(uv)-center)/bkStarsParams.z;
    dx/=bkStarsParams.z; dy/=bkStarsParams.z;
    float core=filteredStar(d,dx,dy,vec2(0.011));
    float halo=filteredStar(d,dx,dy,vec2(0.056))*0.12;
    float rays=filteredStar(d,dx,dy,vec2(0.008,0.045))+filteredStar(d,dx,dy,vec2(0.045,0.008));
    float twinkle=0.2+0.8*pow(0.5+0.5*sin(bkStarsParams.x*(1.2+seed)+seed*113.0),2.0);
    return (core+halo+rays*0.32)*twinkle*lod*(1.0-smoothstep(24.0,64.0,bkDistance));
}
