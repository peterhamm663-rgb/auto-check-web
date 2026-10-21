const {app,BrowserWindow,ipcMain,dialog}=require('electron');
const path=require('path');
const fs=require('fs');
let win;
function createWindow(){
 win=new BrowserWindow({width:1250,height:850,minWidth:800,minHeight:550,autoHideMenuBar:true,webPreferences:{preload:path.join(__dirname,'preload.js'),contextIsolation:true,nodeIntegration:false,sandbox:true}});
 win.loadFile('index.html');
 win.webContents.setWindowOpenHandler(()=>({action:'deny'}));
 win.webContents.on('will-navigate',(event,url)=>{if(!url.startsWith('file://'))event.preventDefault();});
}
app.whenReady().then(()=>{createWindow();app.on('activate',()=>{if(BrowserWindow.getAllWindows().length===0)createWindow();});});
app.on('window-all-closed',()=>{if(process.platform!=='darwin')app.quit();});
ipcMain.handle('autocheck:print',async(_event,html)=>{
 const w=new BrowserWindow({show:false,webPreferences:{sandbox:true,nodeIntegration:false}});
 await w.loadURL('data:text/html;charset=utf-8,'+encodeURIComponent('<!doctype html><html><head><meta charset="utf-8"><style>body{font-family:Arial,sans-serif;padding:18px}table{width:100%}</style></head><body>'+html+'</body></html>'));
 await new Promise(resolve=>w.webContents.once('did-finish-load',resolve));
 w.show();w.webContents.print({silent:false,printBackground:true},()=>{});
});
ipcMain.handle('autocheck:backup',async(_event,base64)=>{
 const {canceled,filePath}=await dialog.showSaveDialog(win,{title:'Guardar respaldo',defaultPath:'autocheck-respaldo-cifrado.json',filters:[{name:'JSON',extensions:['json']}]});
 if(!canceled&&filePath){fs.writeFileSync(filePath,Buffer.from(base64,'base64'));return true;}return false;
});
