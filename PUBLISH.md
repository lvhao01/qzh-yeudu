# Yuedu SDK 发布到 Maven Central 指南

本指南说明如何将 Yuedu SDK 的 5 个模块手动发布到 Sonatype OSSRH，并最终同步到 Maven Central。


## 发布步骤

### 第一步：生成发布文件

在项目根目录执行：

```bash
cd readsdk
./gradlew :yuedu:publishAllPublicationsToLocalStagingRepository
```

**说明：**
- 该命令会构建所有 5 个模块（主插件 + 4 个 AAR）
- 对所有文件进行 GPG 签名
- 生成的文件保存在：`readsdk/yuedu/build/maven-local/io/github/lvhao01/`

**生成的模块：**
1. `lv-yuedu-plugin` (主插件)
2. `lv-yuedu-basesdk`
3. `lv-yuedu-vtbrsdk`
4. `lv-yuedu-vtloginsdk`
5. `lv-yuedu-offlinefinger`

每个模块包含：
- `.aar` 文件（主文件）
- `.pom` 文件（Maven 元数据）
- `.asc` 文件（GPG 签名）
- 校验和文件（md5, sha1, sha256, sha512）

### 第二步：验证生成的文件

检查生成的文件结构：

```
readsdk/yuedu/build/maven-local/
└── io/
    └── github/
        └── lvhao01/
            ├── lv-yuedu-plugin/
            │   └── 1.0.1/
            │       ├── lv-yuedu-plugin-1.0.1.aar
            │       ├── lv-yuedu-plugin-1.0.1.aar.asc
            │       ├── lv-yuedu-plugin-1.0.1.pom
            │       ├── lv-yuedu-plugin-1.0.1.pom.asc
            │       └── ... (校验和文件)
            ├── lv-yuedu-basesdk/
            │   └── 1.0.1/
            │       └── ...
            ├── lv-yuedu-vtbrsdk/
            │   └── 1.0.1/
            │       └── ...
            ├── lv-yuedu-vtloginsdk/
            │   └── 1.0.1/
            │       └── ...
            └── lv-yuedu-offlinefinger/
                └── 1.0.1/
                    └── ...
```

**验证要点：**
- ✅ 所有 `.aar` 文件都有对应的 `.asc` 签名文件
- ✅ 所有 `.pom` 文件都有对应的 `.asc` 签名文件
- ✅ POM 文件内容正确（groupId、artifactId、version）

### 第三步：登录 Sonatype Nexus

访问：https://central.sonatype.com/publishing/deployments

### 第四步：上传文件到 Staging Repository

有两种方式上传：

#### 方式 1：使用 Web UI 上传（推荐）

1. 点击 **Upload** → **Upload Maven2 Artifacts**

2. 选择上传模式：
   - **Bundle Upload**: 上传整个目录（推荐）
   - **Artifact Upload**: 逐个上传文件

3. **Bundle Upload 方式：**
   - 选择整个目录：`readsdk/yuedu/build/maven-local/io/github/lvhao01/`
   - 或者打包成 ZIP 后上传：
     ```bash
     cd readsdk/yuedu/build/maven-local
     # Windows PowerShell
     Compress-Archive -Path io -DestinationPath artifacts.zip
     # Linux/Mac
     zip -r artifacts.zip io/
     ```
   - 然后上传 `artifacts.zip`

4. **Artifact Upload 方式：**
   - 逐个上传每个模块的版本目录
   - 例如：
     - POM 文件：`io/github/lvhao01/lv-yuedu-plugin/1.0.1/lv-yuedu-plugin-1.0.1.pom`
     - AAR 文件：`io/github/lvhao01/lv-yuedu-plugin/1.0.1/lv-yuedu-plugin-1.0.1.aar`
     - 签名文件：`*.asc`
     - 校验和文件：`*.md5`, `*.sha1`, `*.sha256`, `*.sha512`

5. 上传后，系统会自动创建一个 Staging Repository

#### 方式 2：使用 Maven 命令上传

```bash
cd readsdk/yuedu/build/maven-local

# 上传主插件
mvn deploy:deploy-file \
  -Durl=https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/ \
  -DrepositoryId=sonatype-ossrh \
  -DpomFile=io/github/lvhao01/lv-yuedu-plugin/1.0.1/lv-yuedu-plugin-1.0.1.pom \
  -Dfile=io/github/lvhao01/lv-yuedu-plugin/1.0.1/lv-yuedu-plugin-1.0.1.aar \
  -Dpackaging=aar \
  -Dfiles=io/github/lvhao01/lv-yuedu-plugin/1.0.1/lv-yuedu-plugin-1.0.1.aar.asc \
  -Dtypes=asc.asc \
  -Dclassifiers=

# 需要配置 Maven settings.xml 中的 server 凭证
```

**注意：** 需要先在 `~/.m2/settings.xml` 中配置凭证：
```xml
<settings>
  <servers>
    <server>
      <id>sonatype-ossrh</id>
      <username>你的用户名</username>
      <password>你的密码</password>
    </server>
  </servers>
</settings>
```

### 第五步：Close Staging Repository

1. 在 **Staging Repositories** 列表中找到刚创建的 repository（通常名称以 `iogh` 开头）
2. 选中该 repository
3. 点击 **Close** 按钮
4. 系统会自动验证：
   - ✅ GPG 签名有效性
   - ✅ POM 文件完整性
   - ✅ 文件结构正确性
   - ✅ 依赖关系有效性

5. 等待验证完成（通常几分钟）
6. 如果有错误，查看错误信息并修复，然后重新上传

**常见错误：**
- ❌ GPG 签名无效 → 检查签名配置和密钥
- ❌ POM 文件缺少必要字段 → 检查 POM 配置
- ❌ 依赖无法解析 → 确保所有依赖都已发布或可用

### 第六步：Release 到 Maven Central

1. 验证通过后，选中已 Close 的 repository
2. 点击 **Release** 按钮
3. 确认发布（会弹出确认对话框）
4. 点击 **Release** 确认

**注意：** Release 是不可逆操作，请确保所有文件都正确后再执行。

### 第七步：等待同步

发布后需要等待 Sonatype 同步到 Maven Central：

- **时间：** 通常 2-4 小时，最长可能需要 1 天
- **查看状态：** 可以在 https://search.maven.org/ 搜索你的 artifact

**搜索示例：**
- `io.github.lvhao01:lv-yuedu-plugin`
- `io.github.lvhao01:lv-yuedu-basesdk`

## 验证发布成功

### 在 Maven Central 搜索

访问：https://search.maven.org/

搜索你的 groupId：`io.github.lvhao01`

应该能看到所有 5 个模块：
- `lv-yuedu-plugin:1.0.1`
- `lv-yuedu-basesdk:1.0.1`
- `lv-yuedu-vtbrsdk:1.0.1`
- `lv-yuedu-vtloginsdk:1.0.1`
- `lv-yuedu-offlinefinger:1.0.1`

### 在项目中使用

发布成功后，可以在项目中使用：

```kotlin
dependencies {
    // 使用主插件（推荐，包含所有功能）
    implementation("io.github.lvhao01:lv-yuedu-plugin:1.0.1")
    
    // 或单独使用各个模块
    implementation("io.github.lvhao01:lv-yuedu-basesdk:1.0.1")
    implementation("io.github.lvhao01:lv-yuedu-vtbrsdk:1.0.1")
    implementation("io.github.lvhao01:lv-yuedu-vtloginsdk:1.0.1")
    implementation("io.github.lvhao01:lv-yuedu-offlinefinger:1.0.1")
}
```

**确保在 `settings.gradle.kts` 中配置了 Maven Central 仓库：**
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()  // 需要添加这个
    }
}
```

```bash
# 1. 先发布 4 个 AAR 到本地 Maven（首次或更新 AAR 后）
./gradlew :yuedu:publishAarDepsToMavenLocal

# 2. 生成所有发布文件（签名 + 打包）
./gradlew :yuedu:publishAllPublicationsToLocalStagingRepository

# 3. 检查生成的文件
# 文件位置：readsdk/yuedu/build/maven-local/io/github/lvhao01/

# 4. 手动上传到 Sonatype Nexus Web UI
# https://s01.oss.sonatype.org/

# 5. 在 Nexus 中 Close → Release

# 6. 等待同步到 Maven Central（2-4 小时）

修改了 yuedu/build.gradle.kts 、 gradle.properties 、settings.gradle.kts 三个包
```

## 相关链接

- Sonatype OSSRH: https://s01.oss.sonatype.org/
- Sonatype JIRA: https://issues.sonatype.org/
- Maven Central Search: https://search.maven.org/
- GPG 下载: https://www.gpg4win.org/
- 语义化版本: https://semver.org/

## 联系支持

如果遇到问题：
1. 查看 Sonatype 官方文档：https://central.sonatype.org/
2. 在 Sonatype JIRA 提交 issue：https://issues.sonatype.org/
3. 检查项目 GitHub Issues：https://github.com/lvhao01/qzh-yeudu/issues

---

**最后更新：** 2025-01-06
**维护者：** lvhao01 <1274714546@qq.com>

