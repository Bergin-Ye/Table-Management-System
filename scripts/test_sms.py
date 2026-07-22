import json, urllib.request, datetime, hashlib, hmac, uuid, base64

# ====== 配置 ======
ACCESS_KEY_ID = "YOUR_ACCESS_KEY_ID"
ACCESS_KEY_SECRET = "YOUR_ACCESS_KEY_SECRET"
PHONE = "15919958627"
SIGN_NAME = "深圳市昊昱精密机电"
TEMPLATE_CODE = "SMS_510625124"

# 生成验证码
code = "123456"
template_param = json.dumps({"code": code}, ensure_ascii=False)

# ====== 构造请求 ======
# API 地址（RPC 风格，HTTP GET）
host = "dysmsapi.aliyuncs.com"
action = "SendSms"
version = "2017-05-25"
body = f"PhoneNumbers={PHONE}&SignName={urllib.parse.quote(SIGN_NAME, safe='')}&TemplateCode={TEMPLATE_CODE}&TemplateParam={urllib.parse.quote(template_param, safe='')}"

# 用 Python 的 urllib 直接 POST（不走 ACS3 签名，用简单方式）
# 阿里云也支持通过 SDK 发送，我们直接用 pip 安装
import subprocess, sys
subprocess.run([sys.executable, "-m", "pip", "install", "aliyun-python-sdk-core", "aliyun-python-sdk-dysmsapi", "-q"], capture_output=True)

from aliyunsdkcore.client import AcsClient
from aliyunsdkdysmsapi.request.v20170525 import SendSmsRequest

client = AcsClient(ACCESS_KEY_ID, ACCESS_KEY_SECRET, "cn-hangzhou")
request = SendSmsRequest.SendSmsRequest()
request.set_PhoneNumbers(PHONE)
request.set_SignName(SIGN_NAME)
request.set_TemplateCode(TEMPLATE_CODE)
request.set_TemplateParam(template_param)

try:
    response = client.do_action_with_exception(request)
    result = json.loads(response)
    print(json.dumps(result, indent=2, ensure_ascii=False))
    if result.get("Code") == "OK":
        print(f"\n✅ 短信发送成功！验证码：{code}")
    else:
        print(f"\n❌ 发送失败: {result.get('Message', '')}")
except Exception as e:
    print(f"❌ 错误: {e}")
