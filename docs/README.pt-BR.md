[English](README.md) | [简体中文](../README.md) | [Tiếng Việt](README.vi.md) | [हिन्दी](README.hi.md) | **Português (BR)** | [Español](README.es.md)

---

# LowFace - Demo de Reconhecimento Facial Leve

> Um aplicativo de reconhecimento facial projetado especificamente para **dispositivos Android de baixo custo**, implementado com XML/View nativo para verificar a viabilidade em hardware com recursos limitados.

## Sobre o LowFace
* LowFace: Reconhecimento Facial Eficiente em Dispositivos de Baixo Custo

---

**📝 Esta tradução está em andamento.**

Se você é fluente em português brasileiro, agradecemos sua contribuição para completar esta documentação. Por favor, consulte a [versão em inglês](README.md) ou a [versão em chinês](../README.md) para o conteúdo completo.

---

## Funcionalidades Principais

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

## Tecnologia

- **UI**: XML/View nativo (sem Compose)
- **Câmera**: CameraX + PreviewView
- **Detecção Facial**: Google ML Kit
- **Extração de Features**: Modelo EdgeFace TFLite

## Compilação

```powershell
cd lowFace
.\gradlew.bat assembleDebug
```

---

## Valor do Projeto

O LowFace visa explorar a capacidade de **realizar reconhecimento facial utilizável em dispositivos Android de baixo custo**, permitindo que mais dispositivos existentes tenham capacidades de autenticação digital.

Para muitos países em desenvolvimento, áreas remotas e empresas sensíveis a custos, muitos cenários de verificação de identidade não requerem sistemas de reconhecimento facial de nível financeiro ou de segurança, mas precisam de uma solução leve:

- Baixo custo
- Pode funcionar offline
- Baixa dependência de rede
- Pode ser implantado em dispositivos existentes

Exemplos incluem:
- Controle de ponto e registro de funcionários
- Gerenciamento de pessoal em pequenas organizações
- Confirmação de identidade em cenários de treinamento
- Controle de acesso básico e autorização de dispositivos
- Verificação de identidade em serviços comunitários

---

## Contribuições

Agradecemos contribuições de tradução! Por favor, crie um Pull Request para melhorar esta tradução.

## Licença

O SDK principal (`simface`, `simq`) segue a licença do projeto original.

O código da camada de aplicativo é licenciado sob a Licença MIT.
