import { User, UserStates } from "./user";

interface ClientConnectRequest {
	type: string;
	username: string;
	message: string;
}

interface ClientRequest {
	type: string;
	message: string;
	uuid?: string;
}

interface Static {
	type: string;
	uuid: string;
	server: string;
}

interface ServerResponse {
	type: string;
	message: string;
	username: string;
	timestamp: number;
	staticFiles?: Static;
}
interface ChatHistory {
	chatHistory: Array<ServerResponse>;
}

export class Chat {
	private static CHATAREA: HTMLDivElement = document.getElementById("chat_area") as HTMLDivElement;
	private static readonly DEFAULT_URI: string = "ws://" + location.hostname + ":8080/ws";

	private socket?: WebSocket;
	private user: User;

	constructor(user: User) {
		this.user = user;
	}

	private renderMessage(message: ServerResponse) {
		const datetimeLocale = new Date(message.timestamp).toLocaleString();
		switch (message.type) {
			case "systemMessage":
				const systemMessage: string =
					"[" + datetimeLocale + "] " + "[" + message.username + "]: " + message.message;
				this.updateChatField("red", systemMessage);
				break;
			case "userMessage":
				if (message.staticFiles) {
					const staticFiles = message.staticFiles;
					const userMessage: string =
						"[" + datetimeLocale + "] " + "[" + message.username + "]: " + message.message + "\n";
					this.updateChatField(
						"black",
						userMessage,
						"http://" + staticFiles.server + "/static/images/" + staticFiles.uuid + ".png",
					);
				} else {
					const userMessage: string =
						"[" + datetimeLocale + "] " + "[" + message.username + "]: " + message.message;
					this.updateChatField("black", userMessage);
				}
				break;
			default:
				break;
		}
	}

	private updateChatField(color: string, message: string, url?: string) {
		const messageDiv = document.createElement("p");
		messageDiv.style.color = color;
		messageDiv.innerText = message;

		if (url) {
			const messageImg = new Image();
			messageImg.src = url;
			messageDiv.appendChild(messageImg);
		}

		Chat.CHATAREA.appendChild(messageDiv);
		Chat.CHATAREA.scrollTo(0, Chat.CHATAREA.clientHeight)
	}

	private renderChatHistory(history: ChatHistory) {
		for (const message of history.chatHistory) {
			this.renderMessage(message);
		}
	}

	private clearChat() {
		Chat.CHATAREA.innerHTML = "";
	}

	public isConnected() {
		return this.user.isUserConnected();
	}

	public connect(uri: string): Promise<boolean> {
		return new Promise((resolve, reject) => {
			if (this.user.isUserConnected()) {
				alert("You already connected to this room!");
				resolve(true);
				return true;
			}

			if (uri === "") {
				uri = Chat.DEFAULT_URI;
			}

			this.socket = new WebSocket(uri);

			this.socket.onopen = () => {
				this.sendClientConnectRequest("connect", this.user.getUsername(), "connect!");
				this.user.updateUserState(UserStates.CONNECTED);
				resolve(true);
				return true;
			};

			this.socket.onmessage = (event: MessageEvent) => {
				const message = JSON.parse(event.data);
				switch (message.type) {
					case "chatHistory":
						this.renderChatHistory(message as ChatHistory);
						break;
					default:
						this.renderMessage(message as ServerResponse);
						break;
				}
			};

			this.socket.onerror = (event: Event) => {
				console.log(event);
				reject(false);
				return false;
			};

			this.socket.onclose = () => {
				this.user.updateUserState(UserStates.DISCONNECTED);
				this.clearChat();
			};

		});
	}

	public disconnect() {
		if (!this.socket) {
			return;
		}
		this.sendClientRequest("disconnect", "disconnect");
		this.socket.close();
	}

	public sendClientRequest(type: string, message: string, uuid?: string) {
		if (!this.socket) {
			console.log("Socket is undefined");
			return;
		}

		const userMessage: ClientRequest = {
			type: type,
			message: message,
			uuid: uuid,
		};
		const jsonString = JSON.stringify(userMessage);
		this.socket.send(jsonString);
	}

	public sendClientImage(image: ArrayBuffer) {
		if (!this.socket) {
			console.log("Socket is undefined");
			return;
		}
		this.socket.send(image);
	}

	public sendClientConnectRequest(type: string, username: string, message: string) {
		if (!this.socket) {
			console.log("Socket is undefined");
			return;
		}
		const userMessage: ClientConnectRequest = {
			type: type,
			username: username,
			message: message,
		};
		const jsonString = JSON.stringify(userMessage);
		this.socket.send(jsonString);
	}

	public getDefaultUri() {
		return Chat.DEFAULT_URI;
	}
}
