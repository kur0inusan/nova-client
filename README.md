# Nova Client (仮名)

Soar / Glide 系のモダン PvP クライアント。1.8.9 (Legacy Fabric) と 26.3 (Fabric) に対応。

## 構成

| パス | 内容 | Java |
|---|---|---|
| `mod/core` | バージョン非依存のコード(モジュール、設定、HUD、エディタ、Config)。単体テストもここ | 8 |
| `mod/v26` | 26.3 用の層(Renderer / Platform / Mixin) | 25 |
| `mod/v1_8_9` | 1.8.9 用の層(Renderer / Platform / Mixin) | 8 |
| `launcher/` | Tauri ランチャー(未着手) | – |

`v26` と `v1_8_9` は、Loom のバージョンが異なるため独立した Gradle ビルドにしている。どちらも `../core/src/main/java` をソースとして取り込む。core が Java 8 の範囲で書かれていることは、1.8.9 側のビルドで保証される。

## よく使うコマンド

```powershell
cd mod\core;    .\gradlew test        # core のユニットテスト
cd mod\v26;     .\gradlew runClient   # 26.3 開発クライアント
cd mod\v1_8_9;  .\gradlew runClient   # 1.8.9 開発クライアント
.\gradlew build                       # build/libs/nova-*.jar(配布用 jar)
```

- `v26` は Gradle 自体を Java 25 で動かす必要がある。`gradle/gradle-daemon-jvm.properties` で指定済み。

## 新しいモジュールの追加方法

1. `mod/core/src/main/java/dev/kuroinu/nova/module/impl/` にクラスを作る
   - テキスト1行の HUD → `TextHudElement` を継承し、`getText()` だけを実装する
   - 自由に描画する HUD → `HudElement` を継承する
   - HUD 以外 → `Module` を継承し、`on(Events.Tick.class, e -> ...)` でイベントを受け取る
2. 設定は `add(new BooleanSetting(...))` などで宣言する。保存は自動で行われる
3. `Modules.registerDefaults()` に1行追加する

ゲーム内では、**右 Shift** で HUD エディタを開く。操作はドラッグで移動、ホイールで拡大縮小、右クリックで非表示。
