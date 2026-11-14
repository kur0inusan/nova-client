# TargetMarker 仕様

戦っている相手にマーカーを表示する Render カテゴリのモジュール（id: `target_marker`）。
見た目は HTML プレビュー（Artifact「TargetMarker Preview」）で確認・確定したもの。

## ターゲットの決め方

1. **攻撃ターゲット（優先）**: 自分が攻撃した相手。別の相手を攻撃したらそちらに切り替える。
2. **照準ターゲット（予備）**: 攻撃ターゲットがいないとき、クロスヘアで狙っている相手（`Crosshair target` がオンのとき）。照準を外せば消える。
3. 攻撃ターゲットは次のどれかで解除する。
   - 最後の攻撃から `Timeout` 秒が経った
   - 相手が死亡した / ワールドからいなくなった（ログアウト・描画範囲外）
   - ワールド移動・切断
4. 対象の種類は `Players` / `Mobs` / `Animals` で選ぶ。対象外の相手を攻撃してもターゲットにしない。
5. 透明状態の相手と観戦者は常に対象外（自分自身は攻撃も照準もできないので対象にならない）。

種類の判定（1.8.9）: `PlayerEntity` = Players、`Monster`（ゾンビ、スライム、ガストなど）= Mobs、それ以外の生き物 = Animals。防具立ては対象外。

## 見た目（`Mode`）

| モード | 内容 |
|---|---|
| Arrow | 頭上 +0.80 ブロック（ネームタグ +0.5 より上）に ▼。幅 0.34 ブロック、常にカメラ正面を向く。上下 ±0.08 ブロック、周期 1.6 s（sin）。`Arrow spin` オンで Y 軸回転 162°/s |
| Ring | 半径 = 当たり判定の幅/2 + 0.20 ブロック。単色・太さ均一の円、回転なし。足元 +0.03 ↔ 頭 −0.03 を往復 2.0 s（高さ = (1−cos)/2、上下端で減速） |
| Box | 当たり判定 +0.05 ブロックの枠線 + 面を 10% で塗る |

Ring の残像（`Ring trail`）: リングから「直前 250 ms に通った高さのうち最も離れた位置」までをつながった帯で描く。リング側 35% → 40% 地点で 12% → 端で 0% のグラデーション。折り返しでは自然に縮む。

## アニメーション

| 項目 | 値 |
|---|---|
| Fade in | 180 ms / outCubic。alpha 0→1、scale 0.70→1.00 |
| Fade out | 220 ms / inCubic。alpha 1→0、scale 1.00→0.85 |
| ターゲット切替 | 古いマーカーがフェードアウト、新しいマーカーがフェードイン |
| 色の切替（同じ相手で照準→攻撃） | 150 ms / linear で色だけ補間 |
| Hit flash | 攻撃のたびに 250 ms、強さ (1−t)²。白へ 85% 寄せ、scale +18% から戻る |

## 設定

| 設定 | 種類 | デフォルト |
|---|---|---|
| Mode | Arrow / Ring / Box | Arrow |
| Crosshair target | Boolean | ON |
| Timeout | 1〜30 s | 5 s |
| Through walls | Boolean | ON（遮られた部分も 50% の濃さで描画。OFF なら描かない） |
| Hit flash | Boolean | ON |
| Arrow spin | Boolean | OFF |
| Ring trail | Boolean | ON |
| Players / Mobs / Animals | Boolean | ON / ON / OFF |
| Attack color / Look color | Color | #FF5A5F / #F3E7C9 |

## 実装

- core
  - `Events.Attack`（攻撃したエンティティ ID）と `Events.Render3D`（ワールド描画後・手の描画前）
  - `Renderer3D`: カメラ相対座標で三角形・線を描く最小限のインターフェース
  - `Platform.getEntity / getCrosshairEntityId / getWorld`（未対応バージョンはデフォルト実装で無効）
  - `TargetTracker`: ターゲットの選択と解除（単体テストあり）、`TargetMarker`: 描画とアニメーション
- 1.8.9
  - `ClientPlayerInteractionManagerMixin`（`attackEntity`）→ `Events.Attack`
  - `GameRendererMixin`（`renderWorld` の `"hand"` プロファイラ区間の直前）→ `Events.Render3D`
  - `LegacyRenderer3D`: GL11 / Tessellator（POSITION_COLOR）で描画
