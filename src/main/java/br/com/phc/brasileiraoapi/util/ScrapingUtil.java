package br.com.phc.brasileiraoapi.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.phc.brasileiraoapi.dto.PartidaGoogleDTO;

public class ScrapingUtil {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ScrapingUtil.class);

	private static final String BASE_URL_GOOGLE = "https://www.google.com/search?q=";
	
	private static final String COMPLEMENTO_URL_GOOGLE = "&hl=pt-BR";
	
	private static final String CASA = "casa";
	
	private static final String VISITANTE = "visitante";
	
	private static final String DIV_PENALIDADES = "div[class=imso_mh_s__psn-sc]";
	
	private static final String ITEM_GOL = "div[class=imso_gs__gs-r]";
	
	private static final String DIV_GOLS_EQUIPE_CASA = "div[class=imso_gs__tgs imso_gs__left-team]";
	
	private static final String DIV_GOLS_EQUIPE_VISITANTE = "div[class=imso_gs__tgs imso_gs__right-team]";
	
	private static final String DIV_PLACAR_EQUIPE_CASA = "div[class=imso_mh__l-tm-sc imso_mh__scr-it imso-light-font]";
	
	private static final String DIV_PLACAR_EQUIPE_VISITANTE = "div[class=imso_mh__r-tm-sc imso_mh__scr-it imso-light-font]";
	
	private static final String ITEM_LOGO = "img[class=imso_btl__mh-logo]";
	
	private static final String DIV_DADOS_EQUIPE_VISITANTE = "div[class=imso_mh__second-tn-ed imso_mh__tnal-cont imso-tnol]";
	
	private static final String DIV_DADOS_EQUIPE_CASA = "div[class=imso_mh__first-tn-ed imso_mh__tnal-cont imso-tnol]";
	
//	private static final String DIV_DADOS_EQUIPE_CASA = "div[class=imso_mh__first-tn-ed imso_mh__tnal-cont imso-tnol]";
//	
//	private static final String DIV_DADOS_EQUIPE_VISITANTE = "div[class=imso_mh__second-tn-ed imso_mh__tnal-cont imso-tnol]";
	
	private static final String DIV_PARTIDA_ANDAMENTO = "div[class=imso_mh__lv-m-stts-cont]";
	
	private static final String DIV_PARTIDA_ENCERRADA = "span[class=imso_mh__ft-mtch imso-medium-font imso_mh__ft-mtchc]";
	
	private static final String HTTPS = "https:";
	
	private static final String SRC = "src";
	
	private static final String SPAN = "span";
	
	private static final String PENALTIS = "Pênaltis";
	
	public static void main(String[] args) {

		String url = BASE_URL_GOOGLE + "palmeiras+x+corinthiasa+08/08/2020" + COMPLEMENTO_URL_GOOGLE;
		
		ScrapingUtil scraping = new ScrapingUtil();
		
		scraping.obterInformacoesPartida(url);
		
	}

	public PartidaGoogleDTO obterInformacoesPartida(String url) {
		PartidaGoogleDTO partida = new PartidaGoogleDTO();
		
		Document document = null;
		
		try {
			document = Jsoup.connect(url).get();
			
			String title = document.title();
			LOGGER.info("Titulo da pagina: {}", title);
			
			StatusPartida statusPartida = obtemStatusPartida(document);
			LOGGER.info("Status Partida: {}", statusPartida);
			
			if (statusPartida != StatusPartida.PARTIDA_NAO_INICIADA) {
				String tempoPartida = obtemTempoPartida(document);
				LOGGER.info("Tempo partida: {}", tempoPartida);
				
				Integer placarEquipeCasa = recuperarPlacarEquipe(document, DIV_PLACAR_EQUIPE_CASA);
				LOGGER.info("Placar equipe casa: {}", placarEquipeCasa);
				
				Integer placarEquipeVisitante = recuperarPlacarEquipe(document, DIV_PLACAR_EQUIPE_VISITANTE);
				LOGGER.info("Placar equipe visitante: {}", placarEquipeVisitante);
				
				String golsEquipeCasa = recuperaGolsEquipe(document, DIV_GOLS_EQUIPE_CASA);
				LOGGER.info("Gols equipe casa: {}", golsEquipeCasa);
				
				String golsEquipeVisitaante = recuperaGolsEquipe(document, DIV_GOLS_EQUIPE_VISITANTE);
				LOGGER.info("Gols equipe visitante: {}", golsEquipeVisitaante);
				
				Integer placaaEstendidoEquipeCasa = buscarPenalidaes(document, CASA);
				LOGGER.info("placar estendido equipe casa: {}", placaaEstendidoEquipeCasa);
				
				Integer placaaEstendidoEquipeVisitante = buscarPenalidaes(document, VISITANTE);
				LOGGER.info("placar estendido equipe visitante: {}", placaaEstendidoEquipeVisitante);
			}
			
			
			String nomeEquipeCasa = recuperaNomeEquipe(document, DIV_DADOS_EQUIPE_CASA);
			LOGGER.info("Nome Equipe Casa: {}", nomeEquipeCasa);
			
			String nomeEquipeVisitante = recuperaNomeEquipe(document, DIV_DADOS_EQUIPE_VISITANTE);
			LOGGER.info("Nome Equipe Visitante: {}", nomeEquipeVisitante);
			
			String urlLogoEquipeCasa = recuperaLogoEquipe(document, DIV_DADOS_EQUIPE_CASA);
			LOGGER.info("Url logo equipe casa: {}", urlLogoEquipeCasa);
			
			String urlLogoEquipeVisitante = recuperaLogoEquipe(document, DIV_DADOS_EQUIPE_VISITANTE);
			LOGGER.info("Url logo equipe visitante: {}", urlLogoEquipeVisitante);
			
			
			
		} catch (IOException e) {
			LOGGER.error("ERRO AO TENTARA CONECTAR NO GOOGLE COM JSOUP -> {}", e.getMessage());
		}
		
		return partida;
	}
	
	public StatusPartida obtemStatusPartida(Document document) {
		//Situações
		//1 - paratida nao iniciada
		//2 - partida inicia
		//3 - partida encerrada
		//4 - penalidades
		StatusPartida statusPartida = StatusPartida.PARTIDA_NAO_INICIADA;
		
		boolean isTempoPaartida = document.select(DIV_PARTIDA_ANDAMENTO).isEmpty();
		if (!isTempoPaartida) {
			String tempoPartida = document.select(DIV_PARTIDA_ANDAMENTO).first().text();
			statusPartida = StatusPartida.PARTIDA_EM_ANDAMENTO;
			if (tempoPartida.contains(PENALTIS)) {
				statusPartida = StatusPartida.PARTIDA_PENALTIS;
			}
			LOGGER.info(tempoPartida);
		}
		isTempoPaartida = document.select(DIV_PARTIDA_ENCERRADA).isEmpty();
		
		if (!isTempoPaartida) {
			statusPartida = StatusPartida.PARTIDA_ENCERRADA;
		}
		LOGGER.info(statusPartida.toString());
		return statusPartida;
	}
	
	public String obtemTempoPartida(Document document) {
		String tempoPartida = null;
		//Jogo rolando ou intervaalo ou penalaidades
		boolean isTempoPartida = document.select(DIV_PARTIDA_ANDAMENTO).isEmpty();
		if (!isTempoPartida) {
			tempoPartida = document.select(DIV_PARTIDA_ANDAMENTO).first().text();
		}
		
		isTempoPartida = document.select(DIV_PARTIDA_ENCERRADA).isEmpty();
		if (!isTempoPartida) {
			tempoPartida = document.select(DIV_PARTIDA_ENCERRADA).first().text();
		}
		
		LOGGER.info(corrigeTempoPartida(tempoPartida));
		return corrigeTempoPartida(tempoPartida);
	}
	
	public String corrigeTempoPartida(String tempo) {
		if (tempo.contains("'")) {
			return tempo.replace(" ", "").replace("'", " min");
		} else {
			return tempo;
		}
	}
	
	public String recuperaNomeEquipe(Document document, String itemHTML) {
		String nomeEquipe = null;
		Element element = document.selectFirst(itemHTML);
		if (element != null) {
			nomeEquipe = element.select(SPAN).text();
		}
		
		return nomeEquipe;
	}
	
	public String recuperaLogoEquipe(Document document, String itemHTML) {
		String urlLogo = null;
		Element elemento = document.selectFirst(itemHTML);
		if (elemento != null) {
			urlLogo = HTTPS + elemento.select(ITEM_LOGO).attr(SRC);
		}
		
		return urlLogo;
	}
	
	public Integer recuperarPlacarEquipe(Document document, String itemHTML) {
		String placarEquipe = document.selectFirst(itemHTML).text();
		
		return formataPlacarStringInteger(placarEquipe);
	}
	
	
	public String recuperaGolsEquipe(Document document, String itemHTML) {
		List<String> golsEquipe = new ArrayList<>();
		
		Elements elementos = document.select(itemHTML).select(ITEM_GOL);
		
		for (Element e : elementos) {
			String infoGol = e.select(ITEM_GOL).text();
			golsEquipe.add(infoGol);
		}
		
		
		return String.join(", ", golsEquipe);
	}
	
	public Integer buscarPenalidaes(Document document, String tipoEquipe) {
		boolean isPenalidade = document.select(DIV_PENALIDADES).isEmpty();
		if (!isPenalidade) {
			String penalidaades = document.select(DIV_PENALIDADES).text();
			String penalidadesCompleta = penalidaades.substring(0, 5).replace(" ", "");
			String[] divisao = penalidadesCompleta.split("-");
			return tipoEquipe.equals(CASA) ? formataPlacarStringInteger(divisao[0]) : formataPlacarStringInteger(divisao[1]);
		}
		return null;
	}
	
	public Integer formataPlacarStringInteger(String placar) {
		Integer valor;
		try {
			valor = Integer.parseInt(placar);
		} catch (Exception e) {
			valor = 0;
		}
		
		return valor;
	}
}
