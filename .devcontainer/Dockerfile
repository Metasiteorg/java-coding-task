# Not actually used by the devcontainer, but it is used by gitpod
FROM mcr.microsoft.com/devcontainers/java:3-25-trixie
ARG USER=vscode
VOLUME /home/$USER/.m2
VOLUME /home/$USER/.gradle
RUN sudo mkdir -p /home/$USER/.m2 /home/$USER/.gradle && sudo chown $USER:$USER /home/$USER/.m2 /home/$USER/.gradle
