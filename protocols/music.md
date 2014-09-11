# Home automation music player protocol

Music players can implement this protocol to seamlessly integrate in the cherry ecosystem.
Other plugins can send to:music, and listen for from:music messages.

## Messages addressed to music components (to:music)

```json
{
  to: "music"
  body: {
    command: "play"
  }
}
```

### help
```json
{
  command: "help"
}
```

### play
```json
{
  command: "play"
  item: "spotify:track/"
}
```

### resume
```json
{
  command: "resume"
}
```
### pause
```json
{
  command: "pause"
}
```
### toggle
```json
{
  command: "toggle"
}
```

### prev
```json
{
  command: "prev"
}
```
### next
```json
{
  command: "next"
}
```
### shuffle
```json
{
  command: "shuffle"
}
```
### status
```json
{
  command: "status"
}
```
### search
```json
{
  command: "search"
  query: "the beatles"
}
```
### queue_clear
```json
{
  command: "queue_clear"
}
```
### queue_add
```json
{
  command: "queue_add"
  num: <number in the list>
  item: <id or uri>
}
```
### queue_ls
```json
{
  command: "queue_ls"
}
```
### queue_goto
```json
{
  command: "queue_goto"
}
```
### queue_rm
```json
{
  command: "queue_rm"
  item: [1] // indices in the queue
}
```
### playlist_ls
```json
{
  command: "playlist_ls"
}
```
### playlist_ls_1
```json
{
  command: "playlist_ls_1"
  item: 1 # playlist number
}
```
### playlist_ls
```json
{
  command: "queue_add"
}
```
### image
```json
{
  command: "image"
}
```

## Messages sent by music components (from:music)
