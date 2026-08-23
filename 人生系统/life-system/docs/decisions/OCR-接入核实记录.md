# OCR 接入核实记录

日期：2026-08-21

- 百度 OCR 官方高精度通用文字识别使用 OAuth2 `client_credentials` 获取 token，接口为 `POST /rest/2.0/ocr/v1/accurate_basic`，读取 `words_result[].words`。
- 腾讯云通用印刷体识别采用 `TextRecognize`、版本 `2018-11-19` 与 TC3-HMAC-SHA256，读取 `Response.TextDetections[].DetectedText`。
- 阿里云 OCR OpenAPI `RecognizeGeneral` 采用 RPC HMAC-SHA1 参数签名、版本 `2021-07-07`，使用逐页 PNG，不向接口传 PDF。

三家接口入口均在实现当日返回 HTTP 200。项目未引入任何厂商 SDK；实际调用失败会按既定次序降级至本地 OCR。
