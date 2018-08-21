window.onload = async () => {
    const container = document.getElementById("container");

    const loadingMessage = document.createElement("div");
    loadingMessage.innerText = "Loading...";
    container.appendChild(loadingMessage);

    const { status, data } = await (await fetch("/api/inventory")).json();

    const { inventoryEntries } = data;

    if (inventoryEntries.length === 0) {
        const noItems = document.createElement("div");
        noItems.innerText = "You have no items. Try looking around for some!";
        loadingMessage.parentNode.replaceChild(noItems, loadingMessage)
    } else {
        const itemList = document.createElement("ul");
        loadingMessage.parentNode.replaceChild(itemList, loadingMessage);

        inventoryEntries.forEach(({ item, quantity }) => {
            const itemImg = document.createElement("img");
            itemImg.src = item.imgURL;

            const itemInfo = document.createTextNode(`${item.friendlyName} ×${quantity}`);

            const li = document.createElement("li");
            li.appendChild(itemImg);
            li.appendChild(itemInfo);

            itemList.appendChild(li);
        });
    }
};
