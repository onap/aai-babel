beans{
    xmlns cxf: "http://camel.apache.org/schema/cxf"
    xmlns jaxrs: "http://cxf.apache.org/jaxrs"
    xmlns util: "http://www.springframework.org/schema/util"

    infoService(org.onap.aai.babel.service.InfoService)

    util.list(id: 'jaxrsServices') { ref(bean:'infoService') }
}
