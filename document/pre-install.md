## 사전 준비 사항

 — 모든 노드는 CentOs/Rhel 7.2 ~ 7.3 OS 에서 구동됩니다.  
 - 내부망 UDP 뚤려있을것: UDP 가 뚤려있지 않으면 53 포트 domain resolve 가 작동하지 않는다. (운영시)
 - 내부망 TCP 포트는 모두 뚤려있을것 (운영시)
 - 외부망 인바운드 :  퍼블릭 에이전트 머신의 80 포트와 443 포트 (운영시)
 - 외부망 아웃바운드 : 모두 뚤려있을것 (설치시)
 - 네임서버 :  *.<server>.<domain>  형식으로, A MASK 가  *  로 설정된 도메인을 보유하고있을것. (운영시)
 - 모든 서버는 동일한 pem 파일로 ssh 가 가능할 것. (운영시)
 - 모든 서버의 /etc/ssh/ssh_config 는 PermitRootLogin (설치시) 을 허용할 것.
 
### 서버 준비

- 다음의 서버들을 준비해야 합니다. (사양은 추천사항입니다.)
- 마스터 노드는 홀수개의 서버로 준비하도록 합니다. (1,3,5)
- 프라이빗 에이전트 노드는 최소 한개를 구성해야 합니다.
- 퍼블릭 에이전트 노드는 최소 한개를 구성해야 합니다.
- 퍼블릭 에이전트 노드의 80 포트와 443 포트, 그리고 9000 - 60000 (허용 최대치) 포트는 외부에서 접속이 가능해야 합니다.
- 마스터 노드중 한개의 80 포트는 외부에서 접속이 가능해야 합니다.
- 깃랩 서버의 80 포트와 5000 포트는 외부에서 접속이 가능해야 합니다.
- 깃랩 서버, 퍼블릭 에이전트 노드, 그리고 한개의 마스터 노드는 인터넷 환경에서 접속 가능한 퍼블릭 아이피를 가지고 있어야 합니다.

#### 서버 예시

| 역할 / 호스트네임 | 사양                     | IP 주소      | 퍼블릭 IP 주소 | 외부 포트바인딩     |
|-------------------|--------------------------|--------------|----------------|---------------------|
| bootstrap         | 2 CPU /2 GB/100 GB Disk  | 192.168.0.25 |                |                     |
| master1           | 2 CPU /4 GB/100 GB Disk  | 192.168.0.39 | 52.79.125.242  | 80                  |
| master2           | 2 CPU /4 GB/100 GB Disk  | 192.168.0.23 |                |                     |
| master3           | 2 CPU /4 GB/100 GB Disk  | 192.168.0.8  |                |                     |
| public-agent      | 4 CPU /8 GB/100 GB Disk  | 192.168.0.37 | 52.79.51.79    | 80,443,9000 - 60000 |
| agent1            | 4 CPU /8 GB/100 GB Disk  | 192.168.0.27 |                |                     |
| agent2            | 4 CPU /8 GB/100 GB Disk  | 192.168.0.28 |                |                     |
| agent3            | 4 CPU /8 GB/100 GB Disk  | 192.168.0.24 |                |                     |
| agent4            | 4 CPU /8 GB/100 GB Disk  | 192.168.0.25 |                |                     |
| agent5            | 4 CPU /8 GB/100 GB Disk  | 192.168.0.31 |                |                     |
| agent6            | 4 CPU /8 GB/100 GB Disk  | 192.168.0.26 |                |                     |
| agent7            | 4 CPU /8 GB/100 GB Disk  | 192.168.0.33 |                |                     |
| gitlab            | 4 CPU /32 GB/300 GB Disk | 192.168.0.35 | 52.78.60.43    | 80,5000             |
| ci                | 1 CPU /1 GB/100 GB Disk  | 192.168.0.49 |                |                     |

### 호스트 준비


#### 도메인 준비

다음의 도메인들을 준비하도록 합니다. 다음의 보기에서는 pas-mini.io 도메인을 소유했다고 가정하고, 다음의 A_Mask 들을 준비하도록 합니다.

 - gitlab 을 제외한 모든 도메인은 public-agent 서버로 연결토록 합니다.
 - gitlab 도메인은 gitlab 서버로 연결토록 합니다.
 - 외부 인터넷 환경에서 접속이 가능해야 하므로, 퍼블릭 아이피로 연결하도록 합니다.

| A_MASK        | 도메인      | 역할                   | 퍼블릭 아이피              |
|---------------|-------------|------------------------|----------------------------|
| gitlab        | pas-mini.io | 깃랩 / 도커 레지스트리 | 52.78.60.43(깃랩)       |
| config        | pas-mini.io | 클라우드 콘피그 서버   | 52.79.51.79(public-agent) |
| eureka-server | pas-mini.io | 유레카 서버            | 52.79.51.79(public-agent) |
| iam       | pas-mini.io | 사용자 인증 서버       | 52.79.51.79(public-agent) |
| db       | pas-mini.io | 데이터베이스   | 52.79.51.79(public-agent) |
| cloud-server  | pas-mini.io | 클라우드 플랫폼 서버   | 52.79.51.79(public-agent) |
| cloud         | pas-mini.io | 클라우드 플랫폼 UI     | 52.79.51.79(public-agent) |

와일드카드(*) A_Mask 를 활용하실 경우, 다음과 같이 간략히 설정이 가능합니다.

| A_MASK        | 도메인      | 역할                   | 아이피/호스트              |
|---------------|-------------|------------------------|----------------------------|
| gitlab        | pas-mini.io | 깃랩 / 도커 레지스트리 | 52.78.60.43(gitlab)       |
| *        | pas-mini.io | 클라우드 콘피그 서버   | 52.79.51.79(public-agent) |


#### 호스트네임 변경

모든 서버에 각 역할에 맞는 호스트네임으로 변경합니다. 

예) bootstrap 서버

— RHEL : /etc/sysconfig/network 파일을 변경합니다.

```
sudo vi /etc/sysconfig/network

# 다음 한줄의 내용으로 교체
HOSTNAME=bootstrap

# 재시작
sudo reboot
```

— CENTOS : /etc/hostname 파일을 변경합니다.

```
sudo vi /etc/hostname

# 다음 한줄의 내용으로 교체
bootstrap

# 적용
sudo hostname bootstrap
```

#### ssh root 접속 허용

 - 원할한 설치를 위해서 모든 서버에 root 권한으로 ssh 접속이 가능하도록 설정합니다.
 - 보안 이슈가 있을 경우 설치 후에는 설정을 원복하여도 됩니다.
 - 다음 두 파일의 PermitRootLogin 이 no 로 설정되어있을 경우, 주석처리를 하도록 합니다.
   - /etc/ssh/ssh_config
   - /etc/ssh/sshd_config
   
```
sudo vi /etc/ssh/ssh_config
.
.
# PermitRootLogin no
.
.
sudo service sshd restart
```
