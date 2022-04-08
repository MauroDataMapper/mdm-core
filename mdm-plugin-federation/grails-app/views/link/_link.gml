import grails.rest.Link

model {
    Link link
}

Link link = link as Link

link {
    if (link.contentType) contentType link.contentType
    url link.href
}