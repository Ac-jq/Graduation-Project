# 资源媒体存放规划

## 存放位置

- 图片封面：`src/main/resources/static/assets/resources/images`
- 视频素材：`src/main/resources/static/assets/resources/videos`
- 本地文章页：`src/main/resources/static/assets/resources/articles`

## 使用原则

- 数据库中的 `coverUrl` 统一指向后端静态目录下的图片资源。
- 数据库中的 `contentUrl` 根据资源类型区分：
  - `VIDEO` 指向本地 mp4 文件
  - `ARTICLE` 指向本地 html 页面
- 资源列表展示封面图，资源详情页内嵌视频或文章页，不再只跳外链。

## 当前已接入素材

- `breathing-cover.jpg`
- `sleep-cover.jpg`
- `study-cover.jpg`
- `breathing-garden.mp4`
- `campus-rhythm.mp4`
- `sleep-reset-guide.html`
- `exam-rhythm-toolkit.html`

## 后续扩展规范

- 新增图片继续放入 `images`
- 新增视频继续放入 `videos`
- 文章型资源统一做成静态 html 页，并放入 `articles`
- 资源元数据仍然从数据库读取，避免把资源目录结构写死在前端

## 外部来源

- Pexels 图片页：
  - [Green field landscape photo](https://www.pexels.com/photo/green-field-landscape-19925080/)
  - [Cozy bedroom night scene with table lamp](https://www.pexels.com/photo/cozy-bedroom-night-scene-with-table-lamp-28500445/)
  - [Cozy vintage study desk with lamp and books](https://www.pexels.com/photo/cozy-vintage-study-desk-with-lamp-and-books-36286362/)
- MDN 示例视频页：
  - [MDN video and audio content guide](https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Structuring_content/Video_and_audio_content)
  - [MDN video element reference](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/video)
