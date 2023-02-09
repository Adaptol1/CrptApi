import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import java.util.concurrent.TimeUnit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class CrptApi
{
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
//  Окно времени, которым ограничивается заданное количество запросов
    private long timeWindow;
/*  Переменная, которая соответствует доступному количеству запросов
    в заданный промежуток времени. Уменьшается с каждым новым запросом
*/
    private int counter;
//  Время, соответствующее первому запросу в заданном промежутке времени
    private long firstRequestTime;

    public CrptApi (TimeUnit timeUnit, int requestLimit)
    {
        this.timeUnit = timeUnit;
        if(requestLimit > 0)
        {
            this.requestLimit = requestLimit;
            counter = requestLimit;
        }
        else
        {
            throw new IllegalArgumentException("Передано некорректное значение requestLimit");
        }
        timeWindow = TimeUnit.MILLISECONDS.convert(1L, timeUnit);
    }

/*
*   Метод, определяющий превышение лимита запросов к API. Всегда возвращает false,
*   однако останавливает выполнение потока на то время, которое ещё осталось
*   до закрытия текущего окна времени, если лимит превышен
*/
    public synchronized boolean isLimit ()
    {
        long currentTime = System.currentTimeMillis();
        if(counter == requestLimit)
        {
            firstRequestTime = System.currentTimeMillis();
            counter--;
        }
        else if(counter > 0 && currentTime <= (firstRequestTime + timeWindow))
        {
            counter--;
        }
        else if(counter > 0 && currentTime > (firstRequestTime + timeWindow))
        {
            firstRequestTime = System.currentTimeMillis();
            counter = requestLimit;
            counter--;
        }
        else if(counter == 0 && currentTime <= (firstRequestTime + timeWindow))
        {
            counter = requestLimit;
            try{
            Thread.sleep(firstRequestTime + timeWindow - currentTime);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            firstRequestTime = System.currentTimeMillis();
            counter--;
        }
        else if(counter == 0 && currentTime > (firstRequestTime + timeWindow))
        {
            counter = requestLimit;
            firstRequestTime = System.currentTimeMillis();
            counter--;

        }
        return false;
    }

    public void postRequest (CrptDocument document)
    {
        if (isLimit() == false)
        {
            String requestString = documentToJSON(document).toString();
            try {
                final Content postResult = Request.Post(URL)
                        .bodyString(requestString, ContentType.APPLICATION_JSON)
                        .execute().returnContent();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

    }

    private JSONObject documentToJSON (CrptDocument document)
    {
        JSONObject docJSON = new JSONObject();
        if (document.getDescription() != null) {
            JSONObject inn = new JSONObject();
            inn.put("participantInn", document.getParticipantInn());
            docJSON.put("description", inn);
        }
        docJSON.put("doc_id", document.getDocId());
        docJSON.put("doc_status", document.getDocStatus());
        docJSON.put("doc_type", document.getDocType());
        if (document.getImportRequest() != null ) {
            docJSON.put("importRequest", document.getImportRequest());
        }
        docJSON.put("owner_inn", document.getOwnerInn());
        docJSON.put("participant_inn", document.getParticipantInn());
        docJSON.put("producer_inn", document.getProducerInn());
        docJSON.put("production_date", document.getProducerInn());
        docJSON.put("production_type", document.getProductionType());
        Product[] products = document.getProducts();
        if (products != null) {
            JSONArray productsList = new JSONArray();
            for (Product product : products)
            {
                JSONObject productJSON = new JSONObject();
                if (product.getCertificateDocument() != null) {
                    productJSON.put("certificate_document", product.getCertificateDocument());
                } else if (product.getCertificateDocumentDate() != null) {
                    productJSON.put("certificate_document_date", product.getCertificateDocumentDate());
                } else if (product.getCertificateDocumentNumber() != null) {
                    productJSON.put("certificate_document_number", product.getCertificateDocumentNumber());
                }
                productJSON.put("owner_inn", document.getOwnerInn());
                productJSON.put("producer_inn", document.getProducerInn());
                productJSON.put("production_date", document.getProductionDate());
                if (!document.getProductionDate().equals(product.getProductionDate())) {
                    productJSON.put("production_date", product.getProductionDate());
                }
                productJSON.put("tnved_code", product.getTnvedCode());
                if (product.getUitCode() != null) {
                    productJSON.put("uit_code", product.getUitCode());
                } else if (product.getUituCode() != null) {
                    productJSON.put("uitu_code", product.getUituCode());
                } else {
                    throw new IllegalArgumentException("Одно из полей uit_code/uitu_code " +
                            "является обязательным");
                }
                productsList.add(productJSON);
            }
            docJSON.put("products", productsList);
        }
        docJSON.put("reg_date", document.getRegDate());
        docJSON.put("reg_number", document.getRegNumber());
        return docJSON;
    }
/*
*   Класс описывает документ со всеми его параметрами,
*   при этом продукт вынесен как отдельный класс
*/
    public class CrptDocument
    {
        @Getter
        @Setter
        private String description;
        @Getter
        private final String participantInn;
        @Getter
        private final String docId;
        @Getter
        private final String docStatus;
        @Getter
        private final String docType;
        @Getter
        @Setter
        private String importRequest;
        @Getter
        private final String ownerInn;
        @Getter
        private final String producerInn;
        @Getter
        private final String productionDate;
        @Getter
        private final String productionType;
        @Getter
        private final String regDate;
        @Getter
        private final String regNumber;
        @Getter
        @Setter
        private Product[] products;

        public CrptDocument(String description, String participantInn, String docId,
                            String docStatus, String docType, String importRequest,
                            String ownerInn, String producerInn, String productionDate,
                            String productionType, String regDate, String regNumber,
                            Product[] products) {
            this.description = description;
            this.participantInn = participantInn;
            this.docId = docId;
            this.docStatus = docStatus;
            this.docType = docType;
            this.importRequest = importRequest;
            this.ownerInn = ownerInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.regDate = regDate;
            this.regNumber = regNumber;
            this.products = products;
        }
    }

    public class Product
    {
        @Getter
        @Setter
        private String certificateDocument;
        @Getter
        @Setter
        private String certificateDocumentDate;
        @Getter
        @Setter
        private String certificateDocumentNumber;
        @Getter
        @Setter
        private String ownerInn;
        @Getter
        @Setter
        private String producerInn;
        @Getter
        @Setter
        private String productionDate;
        @Getter
        @Setter
        private String tnvedCode;
        @Getter
        @Setter
        private String uitCode;
        @Getter
        @Setter
        private String uituCode;
    }
}