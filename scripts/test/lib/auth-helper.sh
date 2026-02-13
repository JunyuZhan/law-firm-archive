#!/bin/bash

# ═══════════════════════════════════════════════════════════════════════════════
# 认证辅助函数库 - 支持滑块验证的登录
# ═══════════════════════════════════════════════════════════════════════════════
# 用法：在测试脚本中添加以下内容：
#   SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
#   source "$SCRIPT_DIR/lib/auth-helper.sh"
# ═══════════════════════════════════════════════════════════════════════════════

# 发送HTTP请求（如果尚未定义）
if ! type send_request &>/dev/null; then
    send_request() {
        local method=$1
        local url=$2
        local data=$3
        local headers=$4
        
        if [ -n "$data" ]; then
            if [ -n "$headers" ]; then
                curl -s -w "\n%{http_code}" -X "$method" "$url" \
                    -H "Content-Type: application/json" \
                    -H "$headers" \
                    -d "$data" 2>/dev/null
            else
                curl -s -w "\n%{http_code}" -X "$method" "$url" \
                    -H "Content-Type: application/json" \
                    -d "$data" 2>/dev/null
            fi
        else
            if [ -n "$headers" ]; then
                curl -s -w "\n%{http_code}" -X "$method" "$url" \
                    -H "$headers" 2>/dev/null
            else
                curl -s -w "\n%{http_code}" -X "$method" "$url" 2>/dev/null
            fi
        fi
    }
fi

# 检查响应是否成功（只检查第一个 success 字段，避免嵌套响应问题）
auth_check_success() {
    local body=$1
    local success=$(echo "$body" | grep -o '"success":[^,]*' | head -1 | cut -d':' -f2)
    [ "$success" = "true" ]
}

# 完成滑块验证并获取验证令牌
# 返回值：成功时输出 verifyToken，失败时输出空字符串
# 使用方法：local verify_token=$(do_slider_verification "$BASE_URL")
do_slider_verification() {
    local base_url=$1
    
    # Step 1: 获取滑块验证令牌
    local slider_response=$(send_request "GET" "$base_url/auth/slider/token" "" "")
    local slider_body=$(echo "$slider_response" | sed '$d')
    
    if ! auth_check_success "$slider_body"; then
        return 1
    fi
    
    local token_id=$(echo "$slider_body" | grep -o '"tokenId":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$token_id" ]; then
        return 1
    fi
    
    # Step 2: 验证滑块（slideTime 必须在 300ms - 30000ms 之间）
    local verify_response=$(send_request "POST" "$base_url/auth/slider/verify" "{\"tokenId\":\"$token_id\",\"slideTime\":1500}" "")
    local verify_body=$(echo "$verify_response" | sed '$d')
    
    if ! auth_check_success "$verify_body"; then
        return 1
    fi
    
    local verify_token=$(echo "$verify_body" | grep -o '"verifyToken":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$verify_token" ]; then
        return 1
    fi
    
    echo "$verify_token"
}

# 使用滑块验证登录
# 参数：$1=BASE_URL, $2=username, $3=password
# 返回值：成功时输出 accessToken，失败时返回非0
do_login_with_slider() {
    local base_url=$1
    local username=${2:-admin}
    local password=${3:-admin123}
    
    # 完成滑块验证
    local slider_verify_token=$(do_slider_verification "$base_url")
    
    if [ -z "$slider_verify_token" ]; then
        return 1
    fi
    
    # 使用滑块验证令牌登录
    local response=$(send_request "POST" "$base_url/auth/login" "{\"username\":\"$username\",\"password\":\"$password\",\"sliderVerifyToken\":\"$slider_verify_token\"}")
    local body=$(echo "$response" | sed '$d')
    
    if auth_check_success "$body"; then
        local access_token=$(echo "$body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
        echo "$access_token"
        return 0
    else
        return 1
    fi
}
