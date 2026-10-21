const {contextBridge,ipcRenderer}=require('electron');
contextBridge.exposeInMainWorld('AutoCheckAndroid',{printDocument:(html)=>ipcRenderer.invoke('autocheck:print',html),saveBackup:(base64)=>ipcRenderer.invoke('autocheck:backup',base64)});
