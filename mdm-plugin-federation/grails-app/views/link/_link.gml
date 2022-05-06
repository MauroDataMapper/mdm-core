import grails.rest.Link

model {
    Link link
}

Link l = link as Link

link {
    if (l.contentType) contentType l.contentType
    url l.href
}