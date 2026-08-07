# NeoContinuity 移植到 1.12.2 + Actinium 的难度评估

本文基于当前仓库 `26.1/dev` 的 NeoContinuity 源码、`D:\Code\minecraft-1.12.2-src`、`D:\Code\minecraft--26.1.2-sources` 和 `D:\Code\Actinium` 的当前源码整理。

## 结论

这是一次中大型客户端渲染移植，不是简单改包名或换映射。当前项目的 CTM/emissive 核心逻辑大部分可以迁移，但渲染入口、纹理加载、资源扫描和 Mixin 目标基本都要重写。

如果 Actinium 能新增一个小而稳定的“方块 quad 变换”API，移植难度会明显下降。否则只能通过 Mixin 改 `VintageBlockRenderer` 内部实现，风险更高，后续也更容易被 Actinium 重构破坏。

## 当前 NeoContinuity 架构

当前版本基于 NeoForge 26.1.2：

- `BlockStateModel.collectParts(level, pos, state, random, parts)` 已经直接提供世界和方块坐标。
- `CtmBlockStateModel` 和 `EmissiveBlockStateModel` 在模型调用阶段包装原始模型并变换 quad。
- 纹理通过 `SpriteLoader`、`SpriteSourceList`、`AtlasManager` 的 Mixin 注入额外 sprite，并在 `TextureAtlasSprite` 上保存 emissive 对应关系。
- 资源包通过 `AddPackFindersEvent` 注册，通过 `PreparableReloadListener` 控制加载顺序。
- 配置使用 Sodium/NeoForge 配置 API。

这些接口在 1.12.2 中不存在或形态完全不同。

## Actinium 提供的关键能力

Actinium 已经接管了 1.12.2 的地形构建和渲染路径，这对 Continuity 很有价值：

- `WorldSlice` / `ActiniumBlockAccess`：提供线程安全的区块状态快照，包含邻居方块、biome、TileEntity 和光照数据。
- `VintageBlockRenderer`：已经在 `renderBlock(state, pos, blockAccess, layer)` 中拿到世界和坐标，再调用 `model.getQuads(...)`，这是 CTM 运行时变换最自然的插入点。
- `BakedQuadView` / `ModelQuadViewMutable` / `ModelQuadUtil`：可以读取 quad 的坐标、UV、颜色、lightmap、法线和 sprite。
- `TextureMapExtension`：提供 UV 到 `TextureAtlasSprite` 的查找，可用于 SpriteCalculator。
- PBR 的 `AtlasPBRLoader` 提供了按后缀加载额外 sprite 的参考实现，emissive 可以借鉴，但建议仍把 emissive sprite 放进方块 atlas。
- 配置 API：`org.embeddedt.embeddium.api.OptionGUIConstructionEvent`、`OptionPage`、`OptionGroup` 以及 `net.caffeinemc.mods.sodium.api.config.*` 可替代当前配置界面。

## 当前 Actinium API 缺口

`VintageBlockRenderer` 是具体类，没有公开的 “quad transformer” 或 block render hook。当前能直接使用的是世界切片、BakedQuad 视图和配置 API，但 CTM/emissive 需要额外接入点。

建议在 Actinium 中新增类似下面的能力：

```java
public interface BlockQuadTransformer {
    List<BakedQuad> transform(
        IBlockState state,
        BlockPos pos,
        ActiniumBlockAccess blockAccess,
        BlockRenderLayer layer,
        EnumFacing side,
        List<BakedQuad> quads
    );
}
```

然后由 `VintageBlockRenderer` 在调用 `renderQuadList` 前执行已注册的 transformer。这样 Continuity 可以保持独立 mod，不需要 Mixin 进 Actinium 的核心渲染类。

## API 对照表

| NeoContinuity 26.1.2 | 1.12.2 + Actinium |
|---|---|
| `Identifier` | `ResourceLocation` |
| `BlockState` | `IBlockState` |
| `BlockAndTintGetter` | `ActiniumBlockAccess` / `IBlockAccess` |
| `RandomSource` | `long rand` + `Random` / `XoRoShiRoRandom` |
| `TextureAtlasSprite` | 同一个类 |
| `BakedQuad` / `QuadCollection` | `BakedQuad` + 按 `EnumFacing` 的 `List<BakedQuad>` |
| `BlockStateModel.collectParts(...)` | `VintageBlockRenderer.renderBlock(...)` |
| `BlockStateModelPart.getQuads(...)` | `IBakedModel.getQuads(state, side, rand)` |
| `ChunkSectionLayer` | `BlockRenderLayer` |
| `SpriteLoader` / `SpriteSourceList` | `TextureMap` + `TextureStitchEvent` |
| `MultiPackResourceManager` | `SimpleReloadableResourceManager` + `IResourcePack` |
| `ModelEvent.ModifyBakingResult` | `ModelBakeEvent` |
| `AddPackFindersEvent` | FML mod assets 或自定义 `IResourcePack` 注入 |
| `PreparableReloadListener` | `IResourceManagerReloadListener` |
| `FMLPaths.CONFIGDIR` | `Loader.instance().getConfigDir()` |

## 可复用与必须重写的部分

当前仓库共约 104 个 Java 文件。

约 65 个文件属于 CTM 规则、processor、properties、API、缓存和通用工具，主体逻辑可以迁移，但需要替换 MC 类型：

- `api/client`
- `client/properties`
- `client/processor`
- 大部分 `client/util`
- `impl/client`

约 35 到 40 个文件属于版本强相关层，基本需要重写：

- `client/model`：改用 `BakedQuad` 和运行时 quad 变换。
- `client/resource`：改成 1.12.2 资源扫描和 `TextureStitchEvent` 流程。
- `client/mixin`：目标类几乎全部更换。
- `client/config`：改用 Actinium/Sodium 配置 API 或 1.12.2 GUI。
- `client/mixinterface`：按 1.12.2 需要重新设计。

## 主要移植风险

### 1. 渲染入口变化

26.1.2 是在 `collectParts` 中包装模型，1.12.2 的 `IBakedModel.getQuads(state, side, rand)` 拿不到世界和坐标。

Actinium 的 `VintageBlockRenderer` 已经解决了这个问题，因为它自己持有 `ActiniumBlockAccess` 和 `BlockPos`。移植时应把 CTM 变换放在它调用 `renderQuadList` 之前，而不是尝试包装 `IBakedModel`。

### 2. 1.12.2 缺少递归资源扫描

现代 API 可以直接列出 `optifine/ctm` 下的 properties。1.12.2 的 `IResourcePack` 没有 `listResources`，需要新增一个资源包枚举器：

- 遍历 `FMLClientHandler.instance().getResourcePackList()`。
- 遍历 `ResourcePackRepository` 中的用户资源包和 server 资源包。
- 对文件夹包递归读文件。
- 对 zip 包直接枚举 zip entry。
- 保留资源包优先级和 mod 资源包路径。

这是移植中容易被低估的工作。

### 3. 纹理注入

1.12.2 的入口是 `TextureStitchEvent.Pre` / `Post` 和 `TextureMap`：

- 在 `Pre` 中扫描并 `registerSprite` 所有 CTM/emissive 依赖。
- `TextureAtlasSprite` 自带的 `hasCustomLoader` / `load` 可以把 `continuity_reserved/...` 重定向到 `optifine/...`。
- 在 `Post` 中建立普通 sprite 到 emissive sprite 的映射，类似当前 `TextureAtlasSpriteExtension`。

如果直接复用当前 `ResourceRedirectHandler` 的思路，需要把它从 `MultiPackResourceManagerMixin` 改成自定义 `TextureAtlasSprite` 加载器。

### 4. quad 生成

1.12.2 的 `BakedQuadRetextured` 可以重映射 sprite 和 UV，适合普通 CTM 替换。

overlay 和 emissive 需要生成额外 quad。推荐使用 Forge 的 `UnpackedBakedQuad.Builder`，或自定义 `BakedQuad` 子类：

- CTM：`BakedQuadRetextured` 或自定义 retextured quad。
- Overlay：从原 quad 复制几何，替换 sprite/UV/color，并追加到 quad 列表。
- Emissive：复制几何，替换为 emissive sprite，并在 BLOCK 格式的 lightmap UV 中写入满亮度。

Actinium 的 `ModelQuadUtil.mergeBakedLight` 会读取 `BakedQuad` 自带的 lightmap，因此满亮度写在 quad 顶点数据中即可被地形渲染和 Forge 的 `renderLitItem` 正确处理。

### 5. 方块状态和 appearance

现代 `BlockState.getAppearance(...)` 在 1.12.2 没有对应 API。需要用：

- `state.getActualState(blockAccess, pos)`
- `state.getBlock().getExtendedState(state, blockAccess, pos)`

连接判断和 matchBlocks 需要针对 1.12.2 的 `IBlockState`、`IProperty` 和 `Block.REGISTRY` 适配。

### 6. 内置资源包

当前两个内置资源包注册方式不能直接使用。

建议：

- 把 CTM 纹理和 properties 放到 mod assets，例如 `assets/continuity/optifine/ctm/...`。
- 玻璃 pane culling fix 的模型覆盖放入 mod assets。
- 如果必须保留“可选资源包”语义，则要自己注入 `IResourcePack` 到 `ResourcePackRepository`，1.12.2 没有现代的 `AddPackFindersEvent`。

### 7. 物品 emissive

1.12.2 Forge 已支持 `ForgeModContainer.allowEmissiveItems` 和 `ForgeHooksClient.renderLitItem`。

物品模型可以在 `ModelBakeEvent` 或 `ItemModelMesher` 层包装 `IBakedModel`，把带 emissive 的额外 quad 追加到模型输出中。这些 quad 应使用 `DefaultVertexFormats.BLOCK` 并在 lightmap 中写满亮度。

## 建议实施顺序

1. 在 Actinium 增加 `BlockQuadTransformer` API，并让 `VintageBlockRenderer` 调用它。
2. 建立 1.12.2 Cleanroom 项目骨架，依赖 Actinium。
3. 实现资源包枚举和 CTM properties 扫描。
4. 实现 `TextureStitchEvent` 纹理注入和 sprite 映射。
5. 在 `VintageBlockRenderer` hook 中实现 CTM quad 变换。
6. 实现 emissive 方块 quad 和物品模型 emissive。
7. 迁移配置到 Actinium/Sodium 配置 API。
8. 整理内置资源包，验证玻璃、砂岩、书架和玻璃 pane culling fix。
9. 用 Actinium 的开发环境跑真实 CTM/emissive 资源包，并检查光影开启时的表现。

## 验证重点

- 默认 CTM 包在普通地形渲染下正确连接。
- 外部资源包的 `optifine/ctm/*.properties` 能被扫描到。
- overlay 和 multipass 方法与原版 Continuity 行为一致。
- emissive 方块在无光影和有光影时都满亮度。
- emissive 物品模型在 `allowEmissiveItems` 下正确显示。
- 与 Actinium 的 `WorldSlice` 并发构建配合，不在渲染线程外读写主世界。
