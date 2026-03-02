# Sistema de Chat P2P

Este projeto é uma aplicação de mensagens instantâneas descentralizada, onde cada utilizador, chamado de *Peer* atua tanto como cliente como servidor para os demais.

## 🚀 Sobre o Projeto

Ao contrário de modelos tradicionais, este sistema elimina a dependência de um servidor central, utilizando protocolos de comunicação entre *peers* para manter a consistência do chat e a gestão de grupos.

### Pilares da Implementação
- **Descentralização:** Não existe uma figura de "servidor", a lógica de controle é replicada entre os participantes.
- **Descoberta de Nós:** Algoritmos para que os *peers* identifiquem outros utilizadores na rede automaticamente.
- **Eleição de Líderes:** Gestão de estados e decisões distribuídas através de algoritmos de eleição implementados nos *peers*.
- **Replicação de Dados:** Garantia de que a informação de grupos e membros esteja disponível mesmo que alguns nós se desconectem.

## 🛠️ Funcionalidades

- **Gestão de Grupos P2P:** Criar, entrar e listar grupos de forma distribuída.
- **Protocolo APDU:** Troca de mensagens estruturadas para coordenar a rede e o envio de texto.
- **Comunicação TCP/UDP:** Utilização híbrida de sockets para garantir fiabilidade no controlo e rapidez no envio de mensagens.
- **Interface Gráfica (GUI):** Desenvolvida em JavaFX para facilitar a interação do utilizador com o ambiente de rede.

## 📂 Componentes Principais

- `GerenciadorDeEleicao`: Lógica para definição do nó líder no grupo.
- `GerenciadorDeReplicacao`: Sincronização dos estados entre os *peers*.
- `PeerListener`: Escuta constante de novas conexões e mensagens de outros *peers*.

## 💻 Tecnologias Utilizadas

- **Linguagem:** Java
- **Redes:** Sockets TCP e UDP
- **Interface:** JavaFX e CSS
- **Concorrência:** Multi-threading intenso para processar mensagens assíncronas
