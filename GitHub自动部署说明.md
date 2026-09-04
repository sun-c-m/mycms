# GitHub 自动部署到服务器

仓库已经包含 `.github/workflows/deploy.yml`。每次 push 到 `main` 或 `master`，以及在 Actions 页面手动运行时，GitHub Actions 会构建后端、构建管理后台和学院门户、通过 SSH 上传并重启服务。

## 服务器准备

```bash
sudo mkdir -p /opt/cms/{backend,admin/dist,college/dist,deploy,uploads}
sudo apt update && sudo apt install -y nginx openjdk-17-jre
sudo useradd --system --home /opt/cms --shell /usr/sbin/nologin cms || true
sudo chown -R cms:cms /opt/cms
```

准备 MySQL 和 Redis 后，创建 `/etc/cms-backend.env`：

```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://127.0.0.1:3306/cms?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=cms
DB_PASSWORD=请替换为强密码
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
NEWS_UPLOAD_DIR=/opt/cms/uploads
```

创建 `/etc/systemd/system/cms-backend.service`：

```ini
[Unit]
Description=CMS backend
After=network.target
[Service]
User=cms
WorkingDirectory=/opt/cms/backend
EnvironmentFile=/etc/cms-backend.env
ExecStart=/usr/bin/java -jar /opt/cms/backend/mycms.jar
Restart=always
RestartSec=5
[Install]
WantedBy=multi-user.target
```

创建 Nginx 配置 `/etc/nginx/sites-available/cms`，将域名替换为自己的域名：

```nginx
server {
    listen 80;
    server_name example.com;
    root /opt/cms/college/dist;
    index index.html;
    location /api/ { proxy_pass http://127.0.0.1:8081; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; }
    location /admin/ { alias /opt/cms/admin/dist/; try_files $uri $uri/ /admin/index.html; }
    location / { try_files $uri $uri/ /index.html; }
}
```

启用服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now cms-backend
sudo ln -s /etc/nginx/sites-available/cms /etc/nginx/sites-enabled/cms
sudo nginx -t && sudo systemctl reload nginx
```

## GitHub Secrets

在仓库 `Settings -> Secrets and variables -> Actions` 添加：

| Secret | 内容 |
|---|---|
| `SERVER_HOST` | 服务器公网 IP 或域名 |
| `SERVER_USER` | SSH 部署用户名 |
| `SERVER_PORT` | SSH 端口，通常为 22 |
| `SERVER_SSH_KEY` | 部署用户私钥全文 |

私钥不要提交到仓库。服务器需把对应公钥加入部署用户的 `~/.ssh/authorized_keys`。

## 发布

```bash
git add .
git commit -m "deploy: update cms"
git push origin main
```

Actions 完成后访问：

- 门户：`https://example.com/`
- 后台：`https://example.com/admin/`

生产环境还应配置 HTTPS（例如 Certbot），并限制数据库、Redis 和上传目录的网络访问。GitHub 官方文档：[Secrets](https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions)、[Environments](https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment)。
