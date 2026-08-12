[English](README.md) | [简体中文](../README.md) | [Tiếng Việt](README.vi.md) | [हिन्दी](README.hi.md) | **Português (BR)** | [Español](README.es.md)

---

# LowFace - Demo de Reconhecimento Facial Leve

> Um aplicativo de reconhecimento facial projetado especificamente para **dispositivos Android de baixo custo**, implementado com XML/View nativo para verificar a viabilidade em hardware com recursos limitados.

## Sobre o LowFace
* LowFace: Reconhecimento Facial Eficiente em Dispositivos de Baixo Custo

## Origem do Projeto

Este projeto é desenvolvido com base no [Simprints Face Biometrics SDK](https://github.com/Simprints/Biometrics-SimFace), mantendo as capacidades principais de reconhecimento facial enquanto reescreve completamente a camada de UI:

- **Projeto Original**: Construído com Jetpack Compose para UI moderna
- **Este Projeto**: Construído com XML/View nativo, otimizado para dispositivos de baixo custo

## Funcionalidades

- Entrada de ID de funcionário/nome
- Cadastro facial (captura automática quando o limiar de qualidade é atingido)
- Reconhecimento facial (correspondência 1:N)
- Exibição de caixa delimitadora facial em tempo real
- Indicação de pontuação de qualidade

## Parâmetros Principais

| Parâmetro | Valor | Descrição |
|-----------|-------|-----------|
| Limiar de Qualidade | 0.4 | Limiar de julgamento de qualidade facial |
| Limiar de Correspondência | 0.85 | Limiar de correspondência 1:N |
| Dimensão de Features | 512 | Dimensão do embedding de saída EdgeFace |

## Stack Tecnológico

- **UI**: XML/View nativo (sem Compose)
- **Câmera**: CameraX + PreviewView
- **Detecção Facial**: Google ML Kit (via SimFace SDK)
- **Extração de Features**: Modelo EdgeFace TFLite
- **Linguagens**: Java + Kotlin (apenas camada SDK)

## Estrutura do Projeto

```
lowFace/
├── app/                         # Módulo principal da aplicação
│   └── src/main/java/com/low/face/
│       ├── FaceDemoActivity.java       # Activity principal
│       ├── FaceCameraActivity.java     # Activity da câmera
│       ├── FaceEngineManager.java      # Operações principais de face
│       ├── FaceEngineSingleton.java    # Gerenciador singleton
│       ├── FaceStore.java              # Armazenamento em memória
│       ├── FaceRecord.java             # Modelo de dados
│       ├── OverlayView.java            # View de overlay facial
│       └── utils/SimFaceWrapper.kt     # Wrapper Kotlin
├── simface/                     # SDK principal de reconhecimento facial
└── simq/                        # Biblioteca de avaliação de qualidade facial
```

## Compilar e Executar

### Requisitos

- JDK 17+
- Android SDK 33+
- Gradle 9.6.1+

### Comandos de Compilação

```powershell

# Entrar no diretório do projeto
cd lowFace

# Verificação de compilação
.\gradlew.bat compileDebugJavaWithJavac

# Compilar Debug APK
.\gradlew.bat assembleDebug
```

### Instalar e Testar

```powershell
# Instalar no dispositivo
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Ver logs
adb logcat -s FaceDemo:* FaceEngine:* FaceCamera:*
```

---

## Adaptação para Dispositivos de Baixo Custo (Foco Principal)

### Especificações do Dispositivo Alvo

Este projeto é otimizado para dispositivos de baixo custo com as seguintes especificações:

| Item | Especificação |
|------|---------------|
| CPU | MediaTek MT6762 (4 núcleos 2.0GHz) |
| RAM | 2GB |
| Armazenamento | 32GB |
| Versão Android | 10-11 |

### Por que não Compose?

O Jetpack Compose tem os seguintes problemas em dispositivos de baixo custo:

1. **Carregamento Inicial Lento**: Inicialização do runtime Compose + primeira recomposição leva 200-500ms
2. **Alto Uso de Memória**: A biblioteca base do Compose tem cerca de 2-3MB, um peso para dispositivos com 2GB RAM
3. **Latência de Entrada**: Recomposição complexa pode causar lag em campos de entrada
4. **Cold Start Longo**: Tempo do toque no ícone até o estado interativo é maior

Este projeto escolhe **XML/View nativo**:

- Zero overhead de dependências extras
- Otimização de renderização em nível de sistema
- Resposta de entrada mais direta
- Menor pegada de memória

### Otimizações de Performance

#### 1. Otimização de Processamento de Imagem

| Otimização | Solução | Efeito |
|------------|---------|--------|
| Conversão Bitmap | Conversão direta RGBA_8888, pular YUV→JPEG→decode | Economizar ~20ms |
| Liberação ImageProxy | Fechar imediatamente após conversão Bitmap, antes da detecção | Evitar bloqueio do pipeline da câmera |
| Escalonamento de Imagem | Imagem de análise limitada a 480×640 | resizeBitmap leva 0ms |

#### 2. Throttling de Detecção

- Intervalo de detecção: **800ms**
- Usar `AtomicBoolean` para prevenir detecção concorrente
- Frames não detectados são fechados imediatamente, não consumindo CPU

#### 3. Reuso de Resultados

Reusar resultados de detecção de frames de preview durante captura automática para evitar detecção redundante:

```
Antes: Detecção preview → Captura automática → Redetecção(400ms) → Extração de features
Depois: Detecção preview → Captura automática → Extração de features direta
```

Economiza **400-500ms**.

### Dados de Performance em Dispositivo Real

#### Cold Start (Primeira Execução)

| Estágio | Duração |
|---------|---------|
| setContentView | 217-248ms |
| Inicialização da câmera | 267-278ms |
| bindToLifecycle | 278ms |
| Chegada do primeiro frame | 1200-1400ms do onCreate |
| Primeira detecção facial | 1000-1100ms |
| Primeira extração de features | 900-950ms |

#### Operação Estável (Após Aquecimento)

| Estágio | Duração |
|---------|---------|
| Detecção facial | 400-530ms |
| Alinhamento facial | 100-130ms |
| Extração de features | 90-100ms |
| Correspondência 1:N (10 pessoas) | 10-15ms |
| Processamento pós captura automática | ~230ms |

---

## Limitações Atuais

### 1. Velocidade de Detecção Limitada

- **Causa**: Detecção facial ML Kit leva 400-500ms/frame em CPU de baixo custo
- **Impacto**: Não consegue alcançar detecção fluida frame-a-frame em tempo real
- **Estado Atual**: Usando solução de throttling 800ms + captura automática

### 2. Cold Start Lento

- **Causa**: Carregamento de modelo, inicialização OpenCV, escalonamento de frequência CPU
- **Impacto**: Resposta lenta para primeiro cadastro/reconhecimento
- **Estado Atual**: Ainda não há solução perfeita, recomenda-se aquecimento

### 3. Armazenamento em Memória

- **Estado Atual**: Dados cadastrados armazenados apenas em memória
- **Impacto**: Necessidade de recadastrar após reiniciar o app
- **Plano**: Versão futura suportará armazenamento persistente

### 4. Sem Detecção de Vivacidade

- **Estado Atual**: Reconhecimento baseado apenas em fotos
- **Risco**: Pode ser enganado por fotos
- **Plano**: Necessário integrar solução de detecção de vivacidade

### 5. Suporte a Uma Câmera

- **Estado Atual**: Apenas câmera frontal
- **Impacto**: Pode ser inconveniente em alguns cenários
- **Plano**: Suporte futuro para troca de câmera

### 6. Experiência de Entrada Não Totalmente Verificada

- **Estado Atual**: Se campos de entrada permanecem fluidos após inicialização do SDK não totalmente verificado
- **Risco**: Possível latência de entrada em dispositivos de baixo custo
- **Sugestão**: Necessário mais testes para timing "foco ganho → primeiro caractere inserido"

---

## Comparação com Versão Original Compose

| Item | Original (Compose) | Este Projeto (XML/View) |
|------|-------------------|-------------------------|
| Framework UI | Jetpack Compose | XML/View Nativo |
| Carregamento primeira tela | Mais lento | Mais rápido |
| Uso de Memória | Maior | Menor |
| Resposta de Entrada | Pode lagar | Mais fluido |
| Eficiência de Desenvolvimento | Alta | Média |
| Custo de Manutenção | Baixo | Médio |

---

## Direções de Otimização Futura

1. **Armazenamento Persistente**: Usar SQLite ou SharedPreferences para salvar faces cadastradas
2. **Detecção de Vivacidade**: Integrar detecção de piscar/abrir boca
3. **Aquecimento de Câmera**: Pré-aquecer câmera e modelo em background na activity principal
4. **Câmera Traseira**: Suportar troca câmera frontal/traseira
5. **Cadastro em Lote**: Suportar cadastrar múltiplas pessoas de uma vez
6. **Aceleração NPU**: Utilizar NPU para aceleração de inferência se o dispositivo suportar

---

## Licença

O SDK principal (`simface`, `simq`) segue a licença do projeto original.

O código da camada de aplicativo é licenciado sob MIT License, livre para usar e modificar.

---

## Agradecimentos

- [Simprints](https://simprints.com/) - Pelo SDK de reconhecimento facial open source
- [Google ML Kit](https://developers.google.com/ml-kit) - Capacidade de detecção facial
- [EdgeFace](https://github.com/SeetaFace6Open/SeetaFace6Open) - Modelo de extração de features

---

## Valor e Significado do Projeto

Na tecnologia de reconhecimento facial em rápido desenvolvimento hoje, muitas soluções por padrão rodam em dispositivos inteligentes de médio-alto custo ou servidores em nuvem. No entanto, ainda existem muitos cenários de uso com recursos limitados: dispositivos sensíveis a custo, condições de rede limitadas, recursos de computação insuficientes, mas ainda assim necessitando de capacidades básicas de autenticação de identidade.

O objetivo do LowFace não é buscar a mais alta precisão de reconhecimento em ambientes de laboratório, mas explorar **alcançar capacidades de reconhecimento facial utilizáveis em dispositivos Android de baixo custo**, permitindo que mais dispositivos existentes tenham capacidades de autenticação digital.

Para muitos países em desenvolvimento, áreas remotas e empresas sensíveis a custo, muitos cenários de verificação de identidade não requerem sistemas de reconhecimento facial de nível financeiro ou de segurança, mas sim precisam de uma solução leve que seja:

- Baixo custo
- Pode rodar offline
- Baixa dependência de rede
- Pode ser implantado em dispositivos existentes

Exemplos incluem:

- Controle de ponto interno empresarial e registro de funcionários
- Gestão de pessoal em pequenas organizações
- Confirmação de identidade em cenários de treinamento educacional
- Controle de acesso básico e autorização de dispositivos
- Verificação de identidade em serviços comunitários ou de base

Esses cenários focam mais em "confiabilidade e facilidade de implantação" em vez de buscar métricas máximas de reconhecimento em ambientes extremos.

Ao mesmo tempo, o LowFace também foca em estender o ciclo de vida de dispositivos eletrônicos. Muitos dispositivos Android antigos não podem rodar aplicações modernas devido a desempenho insuficiente, mas suas câmeras, telas e capacidades básicas de computação ainda podem atender muitos requisitos de tarefas leves. Através de otimização para hardware de baixo custo, esses dispositivos podem continuar criando valor e reduzir a geração de lixo eletrônico.

De uma perspectiva ambiental, trazer dispositivos antigos de volta a cenários de produção e serviço é essencialmente uma forma de reuso de recursos:

- Reduzir necessidades de compra de novo hardware
- Estender ciclo de uso de dispositivos
- Reduzir lixo eletrônico
- Baixar custos de construção de infraestrutura digital

O LowFace espera explorar uma abordagem técnica mais inclusiva:

> Não atualizar todos os dispositivos para hardware de alta performance, mas permitir que mais dispositivos existentes continuem criando valor através de otimização de software.
> Capacidades avançadas não devem pertencer apenas a dispositivos de alta performance, mas servir mais cenários reais a menor custo e mais amplamente.

Este é o significado de otimização de dispositivos de baixo custo, reconhecimento facial leve e tecnologia de AI na borda no mundo real.
