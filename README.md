# 🚀 CI/CD Pipeline for Spring Boot App with Security & Monitoring

## 📌 Overview
A robust **Jenkins-based CI/CD pipeline** for a Spring Boot backend, integrating security scans, container builds, and Kubernetes deployment. Includes monitoring with **Prometheus**, **Grafana**, and **Alertmanager**.

## 🛠 Features
- CI/CD with Jenkins & automated tests
- **SAST**: SonarQube
- **DAST**: OWASP ZAP
- **Dependency Scan**: OWASP Dependency-Check
- **Container Security**: Trivy (FS & image)
- Docker build & push to **private Nexus registry**
- Deploy to **Kubernetes** with Helm
- Monitoring & alerts with Prometheus + Grafana + Alertmanager

## ⚙️ Tools
Jenkins · SonarQube · OWASP ZAP · OWASP Dependency-Check · Trivy · Nexus · Kubernetes · Helm · Prometheus · Grafana · Alertmanager · Spring Boot

## 🔄 Pipeline Flow
1. Code commit → Jenkins triggers pipeline  
2. Build & test Spring Boot app  
3. SAST scan with SonarQube  
4. Trivy FileSystem Scan
5. Dependency scan with OWASP Dependency-Check  
6. Build Docker image & scan with Trivy  
7. DAST scan with OWASP ZAP
8. Push to Nexus registry
9. Deploy to Kubernetes with Helm  
10. Monitor & alert with Prometheus, Grafana, Alertmanager  

## 📂 Structure
- `k8s-helm/` → Helm charts for Kubernetes deployment  
- `Jenkins` → Jenkins pipeline configurations  
- `src/` → Spring Boot application source code  
- `Dockerfile` → Docker build instructions  
- `README.md` → Project documentation
---

## 🚀 Deployment Steps
```bash
# Build & test
mvn clean install

# Build Docker image
docker build -t nexus.local/myapp:latest .

# Push to Nexus registry
docker push nexus.local/myapp:latest

# Deploy to Kubernetes
helm upgrade --install myapp ./k8s-helm -n springboot-namespace --create-namespace

```

---
## 📊 Monitoring & Alerts

- **Prometheus** → Metrics collection  
- **Grafana** → Dashboards & visualizations  
- **Alertmanager** → Notifications for high CPU, memory, network usage, pod restarts, etc.  
- **Notifications** can be sent to **Email**

