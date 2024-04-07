import Foundation

public struct SQGroup: Identifiable, Codable {
    /// The ID of the group
    public var id: String
    /// The name of the group
    public var name: String
    /// The owner of thr group
    public var owner: SQUser
    /// The default permissions of the group
    public var defaultPermissions: SQDefaultPermissions
    /// The members of the group (and their specific permissions)
    public var members: [SQUserPermissions] = []
    /// The `id`'s of users currently in a live listening session
    public var connectedMembers: [String] = []
    /// Whether or not the group is public
    public var publicGroup: Bool
    /// Whether or not askToJoin is enabled
    public var askToJoin: Bool
    /// (unused) The websocket URL of the server (please use `SharedQSyncManager.connectToGroup` instead!)
    public var wsURL: URL?
    /// The currently playing song
    public var currentlyPlaying: SQSong?
    /// The group's queue
    public var previewQueue: [SQQueueItem] = []
    /// The group's playback state
    public var playbackState: SQPlaybackState?
    /// (unused by client) the URL used to join the group
    public var groupURL: URL?
    /// The `id`'s of users attempting to join the group (if ask to join is enabled)
    public var joinRequests: [String] = []
    public init(id: String, name: String, owner: SQUser, defaultPermissions: SQDefaultPermissions, members: [SQUserPermissions] = [], connectedMembers: [String] = [], publicGroup: Bool, askToJoin: Bool, wsURL: URL? = nil, currentlyPlaying: SQSong? = nil, previewQueue: [SQQueueItem], playbackState: SQPlaybackState? = nil, groupURL: URL? = nil, joinRequests: [String] = []) {
        self.id = id
        self.name = name
        self.owner = owner
        self.defaultPermissions = defaultPermissions
        self.members = members
        self.connectedMembers = connectedMembers
        self.publicGroup = publicGroup
        self.askToJoin = askToJoin
        self.wsURL = wsURL
        self.currentlyPlaying = currentlyPlaying
        self.previewQueue = previewQueue
        self.playbackState = playbackState
        self.groupURL = groupURL
        self.joinRequests = joinRequests
    }
}
public struct SQPlaybackState: Identifiable, Codable {
    public var id: UUID = UUID()
    /// The play/pause state of the currently playing song
    public var state: PlayPauseState
    /// The current timestamp of the currently playing song (note: do not use this for timestamp updates, use `SharedQSyncDelegate.onTimestampUpdate` instead. this should be used for displaying the timestamp to the user)
    public var timestamp: TimeInterval
    public init(id: UUID, state: PlayPauseState, timestamp: TimeInterval) {
        self.id = id
        self.state = state
        self.timestamp = timestamp
    }
    }

public enum PlayPauseState: Int, Codable {
    /// The song is playing
    case play = 0
    /// The song is paused
    case pause = 1
}

public struct SQSong: Identifiable, Codable {
    public var id: String = UUID().uuidString
    /// The title of the song
    public var title: String
    /// The name of the artist(s)
    public var artist: String
    /// A URL to the album art (512x512)
    public var albumArt: URL? = nil
    /// The primary colors in the albumArt. Typically has a count of 2.
    public var colors: [String] = []
    /// (unused)
    public var textColor: String? = nil
    /// The duration of the song
    public var duration: TimeInterval
    public init(id: String = UUID().uuidString, title: String, artist: String, albumArt: URL? = nil, colors: [String] = [], textColor: String? = nil, duration: TimeInterval) {
        self.id = id
        self.title = title
        self.artist = artist
        self.albumArt = albumArt
        self.colors = colors
        self.textColor = textColor
        self.duration = duration
    }
}

public struct SQDefaultPermissions: Codable, Identifiable {
    public var id: String
    /// Controls whether or not members can control playback (play/pause, skip, seek)
    public var membersCanControlPlayback: Bool
    /// Controls whether or not members can add songs to queue
    public var membersCanAddToQueue: Bool
    public init(id: String, membersCanControlPlayback: Bool, membersCanAddToQueue: Bool) {
        self.id = id
        self.membersCanControlPlayback = membersCanControlPlayback
        self.membersCanAddToQueue = membersCanAddToQueue
    }
}

public struct SQUser: Identifiable, Codable {
    /// The user's FirebaseAuth UID
    public var id: String
    /// The user's username
    public var username: String
    /// The user's email (if they signed up using an email address(
    public var email: String?
    /// The `id`s of groups the user is a member of
    public var groups: [String]
    public init(id: String, username: String, email: String? = nil, groups: [String] = []) {
        self.id = id
        self.username = username
        self.email = email
        self.groups = groups
    }
}

public struct SQQueueItem: Identifiable, Codable {
    public var id = UUID().uuidString
    /// The song
    public var song: SQSong
    /// The username of who added the song
    public var addedBy: String
    public init(id: String = UUID().uuidString, song: SQSong, addedBy: String) {
        self.id = id
        self.song = song
        self.addedBy = addedBy
    }
}

public struct SQUserPermissions: Codable, Identifiable {
    public var id: String
    /// The user
    public var user: SQUser
    /// Whether or not the user can control playback
    public var canControlPlayback: Bool
    /// Whether or not the user can add to queue
    public var canAddToQueue: Bool
    /// The last time the user joined a live listening session
    public var lastConnected: Date?
    public init(id: String, user: SQUser, canControlPlayback: Bool, canAddToQueue: Bool, lastConnected: Date? = nil) {
        self.id = id
        self.user = user
        self.canControlPlayback = canControlPlayback
        self.canAddToQueue = canAddToQueue
        self.lastConnected = lastConnected
    }
}

public struct JoinGroupRequest: Codable {
    public var myUID: String
    public var groupID: String
    public init(myUID: String, groupID: String) {
        self.myUID = myUID
        self.groupID = groupID
    }
}

public struct WSMessage: Codable {
    /// The specific message
    public var type: WSMessageType
    /// The data associated with the message
    public var data: Data
    /// The date the message was sent. Use this to ensure a perfect sync
    public var sentAt: Date
    public init(type: WSMessageType, data: Data, sentAt: Date) {
        self.type = type
        self.data = data
        self.sentAt = sentAt
    }
}

public enum WSMessageType: Int, Codable {
    /// The server is sending a group update
    case groupUpdate = 0
    /// A user has skipped this song
    case nextSong = 1
    /// A user has gone back in queue
    case goBack = 2
    /// A user has played the song
    case play = 3
    /// A user has paused the song
    case pause = 4
    /// The server is sending an updated timestamp
    case timestampUpdate = 5
    /// (unused)
    case playbackStarted = 6
    /// A user has seeked to a specific part of the song
    case seekTo = 7
    /// (sent by client) Client is trying to add a song to the queue
    case addToQueue = 8
}


public struct WSPlaybackStartedMessage: Codable {
    public var startedAt: Date
    public init(startedAt: Date) {
        self.startedAt = startedAt
    }
}
public struct WSTimestampUpdate: Codable {
    public var timestamp: TimeInterval
    public var sentAt: Date
    public init(timestamp: TimeInterval, sentAt: Date) {
        self.timestamp = timestamp
        self.sentAt = sentAt
    }
}
