![logo](src/main/resources/assets/continuity/neo_continuity_icon.png)

# NeoContinuity 1.12.2 Vintage ([EN version](#english))

## 🍝赞助

NeoContinuity-Vintage 是 [NeoContinuity](https://github.com/Argon4W/NeoContinuity) 的 1.12.2 移植版, 上游为 Argon4W 所编写的 NeoForge 原生分支, 其基于 PepperCode1 所编写的官方 Continuity.
如果你喜欢这个 MOD, 并且想要支持 NeoContinuity 的开发, 请前往 [爱发电](https://afdian.com/a/argon4w) 为狐狸买一份意面.
也十分感谢 Argon4W 制作精良的 NeoContinuity, 以及 PepperCode1 制作精良的官方 Continuity, 若想支持上游的开发, 请前往 [Buy Me a Coffee](https://buymeacoffee.com/peppercode1) 进行赞助.

## 🖥️模组介绍

NeoContinuity 1.12.2 Vintage 是 [NeoContinuity](https://github.com/Argon4W/NeoContinuity) (NeoForge 原生分支) 的 **1.12.2 移植版**, 基于 Cleanroom Loader, 并将渲染部分接入 Actinium 管线. 它让旧版 Minecraft 也能使用 OptiFine 连接纹理 (CTM) 与发光纹理资源包, 无需 OptiFine; 同时还额外兼容 CTM Mod 格式的贴图驱动资源包 (含 `ctm.json` / `ctm_logic` 自定义真值表). **请勿** 向官方或 NeoContinuity 作者报告任何游玩此模组遇到的问题.

## ✨为什么需要这个MOD

官方 Continuity 仅有 Fabric 版本, 而 [NeoContinuity](https://github.com/Argon4W/NeoContinuity) 带来了原生 NeoForge 分支, 本仓库则是将其带到 1.12.2. 在 1.12.2 上, OptiFine 与 CTM Mod 依赖各自的运行时注入, 与 Actinium 渲染管线存在兼容性冲突. 基于以上原因, 我决定将 Neo 移植到 1.12.2 并原生接入 Actinium, 让旧版客户端也能享受现代连接纹理与发光纹理资源包.

## ⚙️工作原理

NeoContinuity 1.12.2 Vintage 在 1.12.2 上使用 Cleanroom Loader 提供的现代 Java 与 Mixin 能力, 在 NeoContinuity 已移除 Fabric API 依赖的基础上继续适配, 渲染挂载点替换为 Actinium 的 `BlockQuadTransformer` 地形渲染钩子, 通过资源包扫描与 `TextureStitchEvent` 将纹理注入方块图集, 在四边形层面完成连接纹理替换与发光叠加.

## ♻️API 替换:
- Fabric 的 `emitQuads` 方法替换为 Actinium 提供的 `BlockQuadTransformer.transform` 方法.
- Fabric 的 `QuadView` 与 `MutableQuadView` 替换为 `BakedQuad` 与 `MutableQuad` 线段视图.
- Fabric 的 `MutableMesh` 替换为可复用的四边形构建器.
- Fabric 的资源加载与配置 API 替换为 Cleanroom / Forge 原生事件与 JSON 配置.

## ✨特性

- OptiFine 连接纹理: `ctm` / `glass` / `horizontal` / `bookshelf` / `vertical` / `top` / `fixed` / `random` / `repeat`.
- OptiFine 发光纹理: `_e` 后缀发光贴图, 支持方块与物品.
- CTM Mod 格式兼容: `.png.mcmeta` 的 `"ctm"` section (v1 类型) 与 `ctm.json` + `ctm_logic/*.json` 自定义真值表, 含 `proxy` 转发.
- 内置资源包: 默认连接纹理包 (玻璃 / 砂岩 / 书架) 与玻璃板剔除修复包.

<a id="english"></a>
# NeoContinuity 1.12.2 Vintage

## 🍝Sponsorship

NeoContinuity is a 1.12.2 port of [NeoContinuity](https://github.com/Argon4W/NeoContinuity), the native NeoForge fork written by Argon4W based on PepperCode1's official Continuity.
Sponsorships from players can ensure the future ports to other versions. Thanks for everyone that support this MOD! If you like it and want to support my work on development of NeoContinuity, please consider sponsor me at [爱发电](https://afdian.com/a/argon4w).
Also thanks for Argon4W for making such great NeoContinuity, and PepperCode1 for making the official Continuity. If you want to support the upstream development, Please sponsor at [Buy Me a Coffee](https://buymeacoffee.com/peppercode1).

## 🖥️MOD Description

NeoContinuity 1.12.2 Vintage is a **1.12.2 port of [NeoContinuity](https://github.com/Argon4W/NeoContinuity)** (the native NeoForge fork), built on Cleanroom Loader, with rendering integrated into the Actinium pipeline. It brings OptiFine connected textures (CTM) and emissive textures to legacy Minecraft without requiring OptiFine, and additionally supports CTM Mod format texture-driven resource packs (including `ctm.json` / `ctm_logic` custom truth tables). Do **NOT** report issues encountered with this mod to the official or NeoContinuity's author.

## ✨Why need this MOD

The official Continuity is Fabric-only, while [NeoContinuity](https://github.com/Argon4W/NeoContinuity) provides a native NeoForge fork; this repository brings it to 1.12.2. On 1.12.2, OptiFine and CTM Mod rely on their own runtime injection, which conflicts with the Actinium rendering pipeline. For these reasons, I decided to port Neo to 1.12.2 and integrate it natively with Actinium, so legacy clients can enjoy modern connected and emissive texture resource packs.

## ⚙️How it works

On 1.12.2, NeoContinuity 1.12.2 Vintage uses modern Java and Mixin from Cleanroom Loader, building on top of NeoContinuity's Fabric-API-free codebase, and replaces the rendering hooks with Actinium's `BlockQuadTransformer` terrain pipeline. Textures are injected into the block atlas through resource pack scanning and `TextureStitchEvent`, and the connected/emissive replacement happens at the quad level.

## ♻️API Replacement:
- Fabric's `emitQuads` replaced with Actinium's `BlockQuadTransformer.transform` method.
- Fabric's `QuadView` and `MutableQuadView` replaced with `BakedQuad` and `MutableQuad` views.
- Fabric's `MutableMesh` replaced with a reusable quad builder.
- Fabric's resource loading and config APIs replaced with Cleanroom/Forge native events and JSON config.

## ✨Features

- OptiFine connected textures: `ctm` / `glass` / `horizontal` / `bookshelf` / `vertical` / `top` / `fixed` / `random` / `repeat`.
- OptiFine emissive textures: `_e`-suffixed emissive textures for blocks and items.
- CTM Mod format compatibility: `"ctm"` section of `.png.mcmeta` (v1 types) and `ctm.json` + `ctm_logic/*.json` custom truth tables, including `proxy` forwarding.
- Built-in resource packs: default connected textures pack (glass / sandstone / bookshelves) and glass pane culling fix pack.

# Continuity

Continuity is a Minecraft mod that allows resource packs that use the OptiFine connected textures format or OptiFine emissive textures format (only for blocks and item models) to work without OptiFine.

Continuity is client-side only and includes two built-in resource packs. The Default Connected Textures pack provides connected textures for glass, sandstone, and bookshelves, similar to the built-in connected textures provided by OptiFine. The Glass Pane Culling Fix pack culls faces between vertically stacked glass panes to make them look seamless with connected textures.

Formally, Continuity implements the Continuity connected textures specification, Continuity emissive textures specification, and Continuity custom block layers specification. All of these are extensions of the corresponding OptiFine specification and were created to provide more features to resource pack authors. The documentation for the Continuity specifications can be found at the [Continuity wiki](https://github.com/PepperCode1/Continuity/wiki).

Continuity is developed as a Fabric mod and is recommended to be used with Fabric. However, [Connector](https://github.com/Sinytra/Connector) and [Forgified Fabric API](https://github.com/Sinytra/ForgifiedFabricAPI) allow Continuity to work well on other mod loaders such as NeoForge and Forge. Releases are made on CurseForge and Modrinth that are marked as working with these mod loaders; these releases contain the same code as equivalent releases for Fabric, but with additional metadata to declare Connector and Forgified Fabric API as dependencies. An official NeoForge version of Continuity that does not require Forgified Fabric API is not planned at this time due to major technical differences between the Fabric and NeoForge APIs.

### Links

[CurseForge Page](https://www.curseforge.com/minecraft/mc-mods/continuity) \
[Modrinth Page](https://modrinth.com/mod/continuity) \
[Wiki](https://github.com/PepperCode1/Continuity/wiki) \
[Discord](https://discord.gg/7rnTYXu)