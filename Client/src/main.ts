import { Chat } from "./chat";
import { User } from "./user";

import { v4 as uuidv4 } from "uuid";

interface ImageUpload {
    uuid: string;
}

function main() {
    const user: User = new User();
    const chat: Chat = new Chat(user);

    // (document.getElementById("ws_uri") as HTMLInputElement).placeholder = chat.getDefaultUri();
    (document.getElementById("username") as HTMLInputElement).placeholder = user.getDefaultUsername();

    document.getElementById("confirm_username_change")?.addEventListener("click", () => {
        const username: string = (document.getElementById("username") as HTMLInputElement).value;
        user.changeUsername(username);
    });

    document.getElementById("chat_connect")?.addEventListener("click", () => {
        chat.connect(chat.getDefaultUri());
    });

    document.getElementById("chat_disconnect")?.addEventListener("click", () => {
        chat.disconnect();
        alert("You was disconnected!");
    });

    document.getElementById("chat_submit_send_message")?.addEventListener("click", async () => {
        const userMessage: string = (document.getElementById("chat_input") as HTMLInputElement).value;
        const files = (document.getElementById("image_field") as HTMLInputElement).files;

        const uuid = uuidv4();

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
                    console.log(error);
                });
        } else {
            chat.sendClientRequest("userMessage", userMessage);
        }
        (document.getElementById("chat_input") as HTMLInputElement).value = "";
        (document.getElementById("image_field") as HTMLInputElement).value = "";
    });
}

main();
