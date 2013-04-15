package rgms.publication

import org.springframework.dao.DataIntegrityViolationException
//#if($upXMLDissertacao)
import rgms.XMLService
//#end
import rgms.publication.Dissertacao;


class DissertacaoController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index() {
        redirect(action: "list", params: params)
    }

    def list() {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)
        [dissertacaoInstanceList: Dissertacao.list(params), dissertacaoInstanceTotal: Dissertacao.count()]
    }

    def create() {
        [dissertacaoInstance: new Dissertacao(params)]
    }

    def save() {
        def dissertacaoInstance = new Dissertacao(params)
		
		PublicationController pb = new PublicationController()

        if (!pb.upload(dissertacaoInstance) || !dissertacaoInstance.save(flush: true)) {
            render(view: "create", model: [dissertacaoInstance: dissertacaoInstance])
            return
        }
		
		flash.message = messageGenerator('default.created.message', dissertacaoInstance.id)
        redirect(action: "show", id: dissertacaoInstance.id)
    }

    def show() {
        def dissertacaoInstance = Dissertacao.get(params.id)
		

        if (!dissertacaoInstance) {
			flash.message = messageGenerator('default.not.found.message',  params.id)
            redirect(action: "list")
            return
        }

        [dissertacaoInstance: dissertacaoInstance]
    }

    def edit() {
        def dissertacaoInstance = Dissertacao.get(params.id)
        if (!dissertacaoInstance) {
            flash.message = messageGenerator('default.not.found.message',  params.id)
            redirect(action: "list")
            return
        }

        [dissertacaoInstance: dissertacaoInstance]
    }

    def update() {
        def dissertacaoInstance = Dissertacao.get(params.id)
        if (!dissertacaoInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'dissertacao.label', default: 'Dissertacao'), params.id])
            redirect(action: "list")
            return
        }

        if (params.version) {
            def version = params.version.toLong()
            if (dissertacaoInstance.version > version) {
                dissertacaoInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                          [message(code: 'dissertacao.label', default: 'Dissertacao')] as Object[],
                          "Another user has updated this Dissertacao while you were editing")
                render(view: "edit", model: [dissertacaoInstance: dissertacaoInstance])
                return
            }
        }

        dissertacaoInstance.properties = params

        if (!dissertacaoInstance.save(flush: true)) {
            render(view: "edit", model: [dissertacaoInstance: dissertacaoInstance])
            return
        }

		flash.message = messageGenerator('default.updated.message',  dissertacaoInstance.id)
        redirect(action: "show", id: dissertacaoInstance.id)
    }

    def delete() {
        def dissertacaoInstance = Dissertacao.get(params.id)
        if (!dissertacaoInstance) {
			flash.message = messageGenerator('default.not.found.message', params.id)
            redirect(action: "list")
            return
        }

        try {
            dissertacaoInstance.delete(flush: true)
			flash.message = messageGenerator('default.deleted.message', params.id)
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
			flash.message = messageGenerator('default.not.deleted.message' + ' Erro: '+e.message, params.id)
            redirect(action: "show", id: params.id)
        }
    }
	
	def messageGenerator (String code, def id)
	{
		return message(code: code, args: [message(code: 'dissertacao.label', default: 'Dissertacao'), id])
	}
//#if($upXMLDissertacao)
    def uploadXMLDissertacao()
    {
        String flashMessage = 'The non existent dissertations were successfully imported'

        XMLService serv = new XMLService()
        Node xmlFile = serv.parseReceivedFile(request)
        if (!serv.Import(saveDissertations, returnWithMessage, xmlFile, flashMessage))
            return
    }

    Closure returnWithMessage = {
        String msg ->

            redirect(action: "list")
            flash.message = message(code: msg)
    }

    def createDissertation(Node xmlNode) {

            Dissertacao newDissertation = new Dissertacao()
            newDissertation.title =  XMLService.getAttributeValueFromNode(xmlNode, "TITULO-DA-DISSERTACAO-TESE")

            newDissertation.publicationDate = new Date()

            String tryingToParse = XMLService.getAttributeValueFromNode(xmlNode, "ANO-DE-OBTENCAO-DO-TITULO")
            if (tryingToParse.isInteger())
                newDissertation.publicationDate.set(year: tryingToParse.toInteger())

            newDissertation.school = XMLService.getAttributeValueFromNode(xmlNode, "NOME-INSTITUICAO")
            newDissertation.file = 'no File'
            newDissertation.address = 'no Address'
            newDissertation.save(flush: false)

    }

    Closure saveDissertations = {
        Node xmlFile ->
            Node dadosGerais = (Node)xmlFile.children()[0]
            Node mestrado = (Node) ((Node) dadosGerais.children()[3]).children()[1]
            Node doutorado = (Node) ((Node) dadosGerais.children()[3]).children()[2]

            createDissertation(mestrado)
            createDissertation(doutorado)
    }
//#end
}
