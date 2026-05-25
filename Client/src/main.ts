import { Chat } from "./chat";
import { User } from "./user";

interface ImageUpload {
	uuid: string;
}

function main() {
	const user: User = new User();
	const chat: Chat = new Chat(user);

	(document.getElementById("username") as HTMLInputElement).placeholder = user.getDefaultUsername();

	document.getElementById("confirm_username_change")?.addEventListener("click", () => {
		const username: string = (document.getElementById("username") as HTMLInputElement).value;
		user.changeUsername(username);
	});

	document.getElementById("chat_connect")?.addEventListener("click", async (e) => {
		let target = e.currentTarget as HTMLButtonElement;
		if (!chat.isConnected()) {
			let res = await chat.connect(chat.getDefaultUri()).catch(() => { });
			if (!res) return;
			target.innerText = "Disconnect";
		} else {
			chat.disconnect();
			target.innerText = "Connect";
		}
	});

	document.getElementById("chat_submit_send_message")?.addEventListener("click", async () => {
		chatSubmitMessage();
	});

	document.getElementById('chat_input')?.addEventListener('keydown', (e) => {
		// Отправка сообщений при нажатии enter в поле ввода
		if (e.key === 'Enter') {
			e.preventDefault();
			chatSubmitMessage();
		}
	});

	function chatSubmitMessage() {
		if (!chat.isConnected()) {
			alert("You are not connected yet!");
			return;
		}

		const userMessage: string = (document.getElementById("chat_input") as HTMLInputElement).value;
		const files = (document.getElementById("image_field") as HTMLInputElement).files;

		if (files && files.length > 0) {
			const file = files[0];

			fetch("//" + location.hostname + ":8080/upload", {
				method: "Post",
				headers: { "Content-Type": file.type || "application/octet-stream" },
				body: file,
			})
				.then((response) => response.json())
				.then((json) => {
					const uuid: string = (json as ImageUpload).uuid;
					chat.sendClientRequest("userMessage", userMessage, uuid);
				})
				.catch((error) => {
					console.error(error);
				});
		} else {
			chat.sendClientRequest("userMessage", userMessage);
		}

		(document.getElementById("chat_input") as HTMLInputElement).value = "";
		(document.getElementById("image_field") as HTMLInputElement).value = "";
	}

}

main();
