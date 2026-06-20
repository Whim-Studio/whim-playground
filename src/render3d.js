import * as THREE from "three";
import { FACE_FRAMES, FACE_VIEW_BOX } from "./whimFaceFrames.js";

/*
 * 3D render layer for Whim Asteroids.
 *
 * The simulation in main.js still runs entirely in CSS-pixel 2D space (x to the
 * right, y DOWN). This module is a pure view: every frame it reads a snapshot of
 * that simulation state and positions reusable three.js meshes to match. It owns
 * NO gameplay — it never mutates sim state, only renders it.
 *
 * Coordinate mapping: the sim plane is centered at the origin and Y is flipped so
 * "up" is positive. wx = simX - width/2, wy = height/2 - simY. Entities live on
 * the z = 0 play plane; the camera looks at it with a gentle downward tilt and a
 * perspective FOV tuned so the whole field stays visible (see updateCamera).
 *
 * Performance: geometries/materials are shared, meshes are pooled per kind and
 * recycled across frames — no per-frame allocation of GPU resources. dispose()
 * tears everything down.
 */

const TEX_SIZE = 256;

// Build one CanvasTexture per Whim face frame (glyph + permanent angry brows).
// The face is unlit (MeshBasicMaterial) so it glows the same regardless of scene
// lighting, preserving the 2D look. Cached once; the player just swaps which
// texture its face billboard samples.
function buildFaceTextures() {
  const textures = {};
  const vb = FACE_VIEW_BOX;
  for (const [frame, paths] of Object.entries(FACE_FRAMES)) {
    const canvas = document.createElement("canvas");
    canvas.width = TEX_SIZE;
    canvas.height = TEX_SIZE;
    const c = canvas.getContext("2d");
    const draw = TEX_SIZE * 0.74; // glyph footprint, leaves margin for brows
    const scale = draw / vb.width;

    c.save();
    c.translate(TEX_SIZE / 2, TEX_SIZE / 2);
    c.scale(scale, scale);
    c.translate(-vb.width / 2, -vb.height / 2);
    c.fillStyle = "#FF2A1A";
    for (const d of paths) c.fill(new Path2D(d));
    c.restore();

    // Two thick brows angled down toward the center — the "mad" signal, baked in.
    c.save();
    c.translate(TEX_SIZE / 2, TEX_SIZE / 2);
    const s = draw;
    c.strokeStyle = "#7a0000";
    c.lineWidth = Math.max(4, s * 0.07);
    c.lineCap = "round";
    c.beginPath();
    c.moveTo(-s * 0.36, -s * 0.5);
    c.lineTo(-s * 0.08, -s * 0.36);
    c.moveTo(s * 0.08, -s * 0.36);
    c.lineTo(s * 0.36, -s * 0.5);
    c.stroke();
    c.restore();

    const tex = new THREE.CanvasTexture(canvas);
    tex.colorSpace = THREE.SRGBColorSpace;
    tex.anisotropy = 4;
    textures[frame] = tex;
  }
  return textures;
}

// Vertical plum-to-crimson void as the scene background.
function buildBackgroundTexture(colors) {
  const canvas = document.createElement("canvas");
  canvas.width = 4;
  canvas.height = 256;
  const c = canvas.getContext("2d");
  const g = c.createLinearGradient(0, 0, 0, 256);
  g.addColorStop(0, colors.backgroundTop);
  g.addColorStop(0.55, colors.background);
  g.addColorStop(1, colors.backgroundBottom);
  c.fillStyle = g;
  c.fillRect(0, 0, 4, 256);
  const tex = new THREE.CanvasTexture(canvas);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}

// Soft radial sprite used for the lower-center ember glow and bullet halos.
function buildGlowTexture(rgb) {
  const canvas = document.createElement("canvas");
  canvas.width = 128;
  canvas.height = 128;
  const c = canvas.getContext("2d");
  const g = c.createRadialGradient(64, 64, 0, 64, 64, 64);
  g.addColorStop(0, `rgba(${rgb}, 0.9)`);
  g.addColorStop(0.4, `rgba(${rgb}, 0.35)`);
  g.addColorStop(1, `rgba(${rgb}, 0)`);
  c.fillStyle = g;
  c.fillRect(0, 0, 128, 128);
  const tex = new THREE.CanvasTexture(canvas);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}

// Deform an icosphere once so asteroids read as faceted rock instead of a ball.
function buildRockGeometry() {
  const geo = new THREE.IcosahedronGeometry(1, 1);
  const pos = geo.attributes.position;
  const v = new THREE.Vector3();
  for (let i = 0; i < pos.count; i += 1) {
    v.fromBufferAttribute(pos, i);
    const n = 0.78 + ((Math.sin(i * 12.9898) * 43758.5453) % 1) * 0.34;
    v.multiplyScalar(n);
    pos.setXYZ(i, v.x, v.y, v.z);
  }
  geo.computeVertexNormals();
  return geo;
}

// Minimal recycling pool. begin() rewinds, acquire() hands out (creating on
// demand), end() hides whatever wasn't claimed this frame.
class Pool {
  constructor(scene, factory) {
    this.scene = scene;
    this.factory = factory;
    this.items = [];
    this.cursor = 0;
  }
  begin() {
    this.cursor = 0;
  }
  acquire() {
    let item = this.items[this.cursor];
    if (!item) {
      item = this.factory();
      this.items.push(item);
      this.scene.add(item);
    }
    item.visible = true;
    this.cursor += 1;
    return item;
  }
  end() {
    for (let i = this.cursor; i < this.items.length; i += 1) {
      this.items[i].visible = false;
    }
  }
}

export class GameRenderer {
  constructor(canvas, colors, powerups) {
    this.colors = colors;
    this.powerups = powerups;
    this.width = 1;
    this.height = 1;

    this.renderer = new THREE.WebGLRenderer({
      canvas,
      antialias: true,
      powerPreference: "high-performance",
    });
    this.renderer.setClearColor(0x000000, 0);

    this.scene = new THREE.Scene();
    this.scene.background = buildBackgroundTexture(colors);

    this.camera = new THREE.PerspectiveCamera(50, 1, 1, 6000);

    this._initLights();
    this._initStarfield();
    this._initBackdropGlow();

    this.faceTextures = buildFaceTextures();
    this.rockGeometry = buildRockGeometry();
    this.glowTexture = buildGlowTexture("255, 180, 130");

    this._initSharedMaterials();
    this._initPlayer();
    this._initPointerGuide();
    this._initPools();

    this._scratchColor = new THREE.Color();
  }

  _initLights() {
    this.scene.add(new THREE.AmbientLight(0xffffff, 0.55));
    const key = new THREE.DirectionalLight(0xfff0e6, 1.15);
    key.position.set(-0.4, 0.8, 1).multiplyScalar(500);
    this.scene.add(key);
    const rim = new THREE.DirectionalLight(0xff5a4a, 0.6);
    rim.position.set(0.6, -0.5, 0.4).multiplyScalar(500);
    this.scene.add(rim);
    // Travels with the player for a warm local glow on nearby hazards.
    this.playerLight = new THREE.PointLight(0xff4d4d, 1.4, 900, 2);
    this.scene.add(this.playerLight);
  }

  _initStarfield() {
    const count = 900;
    const positions = new Float32Array(count * 3);
    for (let i = 0; i < count; i += 1) {
      positions[i * 3] = (Math.random() - 0.5) * 4200;
      positions[i * 3 + 1] = (Math.random() - 0.5) * 3000;
      positions[i * 3 + 2] = -300 - Math.random() * 2600;
    }
    const geo = new THREE.BufferGeometry();
    geo.setAttribute("position", new THREE.BufferAttribute(positions, 3));
    const mat = new THREE.PointsMaterial({
      color: new THREE.Color(this.colors.star),
      size: 7,
      sizeAttenuation: true,
      transparent: true,
      opacity: 0.85,
      depthWrite: false,
    });
    this.stars = new THREE.Points(geo, mat);
    this.starsMaterial = mat;
    this.scene.add(this.stars);
  }

  _initBackdropGlow() {
    const mat = new THREE.SpriteMaterial({
      map: buildGlowTexture("255, 77, 77"),
      transparent: true,
      opacity: 0.5,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
      depthTest: false,
    });
    this.backdropGlow = new THREE.Sprite(mat);
    this.backdropGlow.position.set(0, -300, -400);
    this.scene.add(this.backdropGlow);
  }

  _initSharedMaterials() {
    const C = this.colors;
    this.materials = {
      asteroid: new THREE.MeshStandardMaterial({
        color: C.cinderEdge,
        emissive: C.hazard,
        emissiveIntensity: 0.4,
        roughness: 0.95,
        metalness: 0.05,
        flatShading: true,
      }),
      cinder: new THREE.MeshStandardMaterial({
        color: C.emberCore,
        emissive: C.moteHot,
        emissiveIntensity: 1.1,
        roughness: 0.6,
        flatShading: true,
      }),
      lance: new THREE.MeshStandardMaterial({
        color: C.emberCore,
        emissive: C.moteHot,
        emissiveIntensity: 1.0,
        roughness: 0.4,
      }),
      drifterBody: new THREE.MeshStandardMaterial({
        color: C.veilCore,
        emissive: C.veilCore,
        emissiveIntensity: 0.7,
        roughness: 0.3,
        transparent: true,
        opacity: 0.6,
        depthWrite: false,
      }),
      drifterGlow: new THREE.MeshBasicMaterial({
        color: C.veilCore,
        transparent: true,
        opacity: 0.18,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
      }),
      bullet: new THREE.MeshBasicMaterial({ color: C.face }),
      bulletStrong: new THREE.MeshBasicMaterial({
        color: this.powerups.strongShots.color,
      }),
    };

    // Shared primitive geometries (scaled per-instance).
    this.geo = {
      ico: new THREE.IcosahedronGeometry(1, 0),
      sphere: new THREE.SphereGeometry(1, 20, 16),
      lance: (() => {
        const g = new THREE.ConeGeometry(0.5, 4, 6);
        g.rotateZ(-Math.PI / 2); // tip points +X so rotation.z = -angle aims it
        return g;
      })(),
      octa: new THREE.OctahedronGeometry(1, 0),
      torus: new THREE.TorusGeometry(1, 0.16, 10, 28),
      bullet: new THREE.IcosahedronGeometry(1, 1),
      plane: new THREE.PlaneGeometry(1, 1),
    };
  }

  _initPlayer() {
    const C = this.colors;
    this.playerGroup = new THREE.Group();

    // Faceted 3D body that sits behind the face glyph for real depth/shading.
    this.playerBody = new THREE.Mesh(
      new THREE.IcosahedronGeometry(1, 1),
      new THREE.MeshStandardMaterial({
        color: "#5a1118",
        emissive: C.face,
        emissiveIntensity: 0.45,
        roughness: 0.55,
        metalness: 0.2,
        flatShading: true,
      }),
    );
    this.playerGroup.add(this.playerBody);

    // Unlit Whim face billboard in front of the body.
    this.faceMaterial = new THREE.MeshBasicMaterial({
      map: this.faceTextures.default,
      transparent: true,
      depthWrite: false,
    });
    this.faceMesh = new THREE.Mesh(this.geo.plane, this.faceMaterial);
    this.playerGroup.add(this.faceMesh);

    // Pulsing pink shield bubble, shown only while the shield power-up is active.
    this.shieldMesh = new THREE.Mesh(
      this.geo.sphere,
      new THREE.MeshBasicMaterial({
        color: "#ff69b4",
        transparent: true,
        opacity: 0.22,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
      }),
    );
    this.playerGroup.add(this.shieldMesh);

    this.scene.add(this.playerGroup);
  }

  _initPointerGuide() {
    const mat = new THREE.LineBasicMaterial({
      color: this.colors.face,
      transparent: true,
      opacity: 0.4,
    });
    const geo = new THREE.BufferGeometry();
    geo.setAttribute(
      "position",
      new THREE.BufferAttribute(new Float32Array(6), 3),
    );
    this.pointerLine = new THREE.Line(geo, mat);
    this.pointerLine.visible = false;
    this.scene.add(this.pointerLine);

    this.pointerRing = new THREE.Mesh(
      new THREE.TorusGeometry(13, 1.2, 8, 24),
      new THREE.MeshBasicMaterial({
        color: this.colors.face,
        transparent: true,
        opacity: 0.5,
      }),
    );
    this.pointerRing.visible = false;
    this.scene.add(this.pointerRing);
  }

  _initPools() {
    const s = this.scene;
    this.pools = {
      asteroid: new Pool(s, () => new THREE.Mesh(this.rockGeometry, this.materials.asteroid)),
      cinder: new Pool(s, () => new THREE.Mesh(this.geo.ico, this.materials.cinder)),
      lance: new Pool(s, () => new THREE.Mesh(this.geo.lance, this.materials.lance)),
      drifter: new Pool(s, () => {
        const g = new THREE.Group();
        g.add(new THREE.Mesh(this.geo.sphere, this.materials.drifterBody));
        const glow = new THREE.Mesh(this.geo.sphere, this.materials.drifterGlow);
        glow.scale.setScalar(1.5);
        g.add(glow);
        return g;
      }),
      bullet: new Pool(s, () => new THREE.Mesh(this.geo.bullet, this.materials.bullet.clone())),
      powerup: new Pool(s, () => new THREE.Mesh(this.geo.octa, new THREE.MeshStandardMaterial({
        emissive: "#ffffff",
        emissiveIntensity: 0.9,
        roughness: 0.3,
        metalness: 0.4,
      }))),
      particle: new Pool(s, () => new THREE.Mesh(this.geo.ico, new THREE.MeshBasicMaterial({
        transparent: true,
        depthWrite: false,
        blending: THREE.AdditiveBlending,
      }))),
      trail: new Pool(s, () => new THREE.Mesh(this.geo.plane, new THREE.MeshBasicMaterial({
        map: this.faceTextures.default,
        color: this.colors.face,
        transparent: true,
        depthWrite: false,
        blending: THREE.AdditiveBlending,
      }))),
    };

    // Friendly helpers are single persistent groups, toggled on/off per frame.
    this.squirrel = this._buildSquirrel();
    this.squirrel.visible = false;
    s.add(this.squirrel);
    this.dolphin = this._buildDolphin();
    this.dolphin.visible = false;
    s.add(this.dolphin);
  }

  _buildSquirrel() {
    const g = new THREE.Group();
    const fur = new THREE.MeshStandardMaterial({ color: "#a8703a", emissive: "#3a2310", emissiveIntensity: 0.3, roughness: 0.9 });
    const body = new THREE.Mesh(this.geo.sphere, fur);
    body.scale.set(0.72, 0.56, 0.56);
    g.add(body);
    const head = new THREE.Mesh(this.geo.sphere, fur);
    head.scale.setScalar(0.42);
    head.position.set(0.66, 0.34, 0);
    g.add(head);
    const tail = new THREE.Mesh(this.geo.sphere, new THREE.MeshStandardMaterial({ color: "#7a4a22", emissive: "#2a1808", emissiveIntensity: 0.3, roughness: 1 }));
    tail.scale.set(0.5, 0.8, 0.5);
    tail.position.set(-0.8, 0.4, 0);
    g.add(tail);
    return g;
  }

  _buildDolphin() {
    const g = new THREE.Group();
    const skin = new THREE.MeshStandardMaterial({ color: "#5c7a99", emissive: "#16242e", emissiveIntensity: 0.3, roughness: 0.6 });
    const body = new THREE.Mesh(this.geo.sphere, skin);
    body.scale.set(1.3, 0.5, 0.5);
    g.add(body);
    const tail = new THREE.Mesh(this.geo.lance, skin);
    tail.scale.set(0.4, 0.5, 0.5);
    tail.position.set(-1.5, 0, 0);
    tail.rotation.z = Math.PI;
    g.add(tail);
    const dress = new THREE.Mesh(this.geo.torus, new THREE.MeshStandardMaterial({ color: "#ff5db1", emissive: "#5a1038", emissiveIntensity: 0.4, roughness: 0.7 }));
    dress.scale.set(0.5, 0.7, 0.7);
    dress.rotation.y = Math.PI / 2;
    dress.position.set(0.1, 0, 0);
    g.add(dress);
    return g;
  }

  // World-space helpers (sim pixel -> centered, Y-up world units).
  _wx(x) {
    return x - this.width / 2;
  }
  _wy(y) {
    return this.height / 2 - y;
  }

  resize(width, height, dpr) {
    this.width = width;
    this.height = height;
    this.renderer.setPixelRatio(Math.min(dpr || 1, 2));
    this.renderer.setSize(width, height, false);
    this.updateCamera();
  }

  updateCamera() {
    const aspect = this.width / this.height;
    this.camera.aspect = aspect;
    // Distance that fits the field vertically; since aspect = w/h this also fits
    // it horizontally. A small Y lift + margin tilts the view for a 3D read while
    // keeping the whole playfield on screen.
    const vFov = (this.camera.fov * Math.PI) / 180;
    const fitDist = this.height / 2 / Math.tan(vFov / 2);
    const margin = 1.16;
    this.camera.position.set(0, this.height * 0.1, fitDist * margin);
    this.camera.lookAt(0, 0, 0);
    this.camera.updateProjectionMatrix();
    this.backdropGlow.scale.setScalar(Math.max(this.width, this.height) * 1.6);
  }

  render(world, now) {
    const t = now * 0.001;

    this._renderStars(t);
    this._renderShards(world.shards);
    this._renderBullets(world.bullets);
    this._renderPowerUps(world.powerUps, now);
    this._renderParticles(world.burstParticles);
    this._renderTrails(world.trails);
    this._renderHelpers(world);
    this._renderPointer(world);
    this._renderPlayer(world, now);

    this.renderer.render(this.scene, this.camera);
  }

  _renderStars(t) {
    this.stars.rotation.z = t * 0.01;
    this.stars.position.x = Math.sin(t * 0.05) * 30;
    this.starsMaterial.opacity = 0.6 + Math.sin(t * 0.8) * 0.12;
  }

  _renderShards(shards) {
    const pools = this.pools;
    pools.asteroid.begin();
    pools.cinder.begin();
    pools.lance.begin();
    pools.drifter.begin();

    for (const shard of shards) {
      const x = this._wx(shard.x);
      const y = this._wy(shard.y);
      if (shard.kind === "cinder") {
        const m = pools.cinder.acquire();
        m.position.set(x, y, 6);
        m.scale.setScalar(shard.radius);
        m.rotation.set(shard.angle, shard.angle * 0.7, -shard.angle);
      } else if (shard.kind === "lance") {
        const m = pools.lance.acquire();
        m.position.set(x, y, 4);
        m.scale.setScalar(shard.radius);
        m.rotation.set(0, 0, -shard.angle);
      } else if (shard.kind === "drifter") {
        const m = pools.drifter.acquire();
        m.position.set(x, y, -8);
        const breath = 1 + Math.sin(shard.pulse) * 0.08;
        m.scale.setScalar(shard.radius * breath);
        m.rotation.z = -shard.angle;
      } else {
        const m = pools.asteroid.acquire();
        m.position.set(x, y, 0);
        m.scale.setScalar(shard.radius);
        m.rotation.set(shard.angle * 0.6, shard.angle, -shard.angle * 0.4);
      }
    }

    pools.asteroid.end();
    pools.cinder.end();
    pools.lance.end();
    pools.drifter.end();
  }

  _renderBullets(bullets) {
    const pool = this.pools.bullet;
    pool.begin();
    for (const bullet of bullets) {
      const m = pool.acquire();
      m.material.color.set(
        bullet.strong ? this.powerups.strongShots.color : this.colors.face,
      );
      m.position.set(this._wx(bullet.x), this._wy(bullet.y), 8);
      m.scale.setScalar(bullet.radius * 1.3);
    }
    pool.end();
  }

  _renderPowerUps(powerUps, now) {
    const pool = this.pools.powerup;
    pool.begin();
    const spin = now * 0.002;
    for (const powerUp of powerUps) {
      const m = pool.acquire();
      const color = this.powerups[powerUp.type]?.color ?? this.colors.face;
      m.material.color.set(color);
      m.material.emissive.set(color);
      m.position.set(this._wx(powerUp.x), this._wy(powerUp.y), 6);
      m.scale.setScalar(powerUp.radius * 1.25);
      m.rotation.set(spin, spin * 1.3, 0);
    }
    pool.end();
  }

  _renderParticles(particles) {
    const pool = this.pools.particle;
    pool.begin();
    for (const p of particles) {
      const m = pool.acquire();
      const life = 1 - p.age / p.life;
      m.material.color.set(p.color);
      m.material.opacity = Math.max(0, life) * 0.9;
      m.position.set(this._wx(p.x), this._wy(p.y), 10);
      m.scale.setScalar(p.size * (0.6 + life * 0.6));
    }
    pool.end();
  }

  _renderTrails(trails) {
    const pool = this.pools.trail;
    pool.begin();
    for (const trail of trails) {
      const m = pool.acquire();
      const progress = trail.age / trail.life;
      m.material.map = this.faceTextures[trail.frame] ?? this.faceTextures.default;
      m.material.opacity = (1 - progress) * 0.16;
      m.position.set(this._wx(trail.x), this._wy(trail.y), -2);
      m.scale.setScalar(trail.size * (1 + progress * 0.12));
      m.rotation.z = -trail.rotation;
    }
    pool.end();
  }

  _renderHelpers(world) {
    const sq = world.squirrel;
    if (sq) {
      this.squirrel.visible = true;
      this.squirrel.position.set(this._wx(sq.x), this._wy(sq.y), 5);
      this.squirrel.scale.set(sq.facing * sq.radius, sq.radius, sq.radius);
      this.squirrel.position.y += Math.sin(sq.runPhase * 2) * 2;
    } else {
      this.squirrel.visible = false;
    }

    const dol = world.dolphin;
    if (dol) {
      this.dolphin.visible = true;
      const faceDir = Math.cos(dol.heading) >= 0 ? 1 : -1;
      this.dolphin.position.set(this._wx(dol.x), this._wy(dol.y) + Math.sin(dol.bob) * 4, 7);
      this.dolphin.scale.set(faceDir * dol.radius, dol.radius, dol.radius);
      this.dolphin.rotation.z = Math.sin(dol.bob) * 0.06;
    } else {
      this.dolphin.visible = false;
    }
  }

  _renderPointer(world) {
    const p = world.pointer;
    if (!p.active || !world.playing) {
      this.pointerLine.visible = false;
      this.pointerRing.visible = false;
      return;
    }
    const px = this._wx(world.player.x);
    const py = this._wy(world.player.y);
    const tx = this._wx(p.x);
    const ty = this._wy(p.y);
    const arr = this.pointerLine.geometry.attributes.position.array;
    arr[0] = px; arr[1] = py; arr[2] = 1;
    arr[3] = tx; arr[4] = ty; arr[5] = 1;
    this.pointerLine.geometry.attributes.position.needsUpdate = true;
    this.pointerLine.visible = true;
    this.pointerRing.visible = true;
    this.pointerRing.position.set(tx, ty, 1);
  }

  _renderPlayer(world, now) {
    const player = world.player;
    const x = this._wx(player.x);
    const y = this._wy(player.y);
    this.playerGroup.position.set(x, y, 0);
    this.playerLight.position.set(x, y, 60);

    // Body sits at the play plane; face billboard floats just in front.
    this.playerBody.scale.setScalar(player.radius * 1.05);
    this.playerBody.rotation.set(now * 0.0006, now * 0.0009, -player.rotation);

    this.faceMaterial.map = this.faceTextures[world.faceFrame] ?? this.faceTextures.default;
    const trembleX = Math.sin(now * 0.05) * 1.4 + Math.sin(now * 0.13) * 0.8;
    const trembleY = Math.cos(now * 0.047) * 1.4 + Math.cos(now * 0.11) * 0.8;
    const faceSize = player.size * (1 + world.shotPulse * 0.05);
    this.faceMesh.scale.setScalar(faceSize);
    this.faceMesh.position.set(trembleX, trembleY, player.radius + 6);
    this.faceMesh.rotation.z = -player.rotation;

    // Flicker the whole player during the post-hit grace window.
    const flicker = world.invulnerable && Math.floor(now / 110) % 2 === 0 ? 0.35 : 1;
    this.faceMaterial.opacity = flicker;
    this.playerBody.material.opacity = flicker;
    this.playerBody.material.transparent = flicker < 1;

    if (world.shieldActive) {
      this.shieldMesh.visible = true;
      const pulse = (Math.sin(now / 120) + 1) / 2;
      this.shieldMesh.scale.setScalar(player.radius * (1.7 + pulse * 0.15));
      this.shieldMesh.material.opacity = 0.16 + pulse * 0.12;
    } else {
      this.shieldMesh.visible = false;
    }
  }

  dispose() {
    this.renderer.dispose();
    this.scene.traverse((obj) => {
      if (obj.geometry) obj.geometry.dispose();
      if (obj.material) {
        const mats = Array.isArray(obj.material) ? obj.material : [obj.material];
        for (const m of mats) {
          if (m.map) m.map.dispose();
          m.dispose();
        }
      }
    });
  }
}
