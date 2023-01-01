//Manage stacks of hierarchical objects for writing burr in Detail
package app.familygem

import app.familygem.detail.*
import org.folg.gedcom.model.*
import java.util.*

class Memory internal constructor() {
    var list: MutableList<StepStack> = ArrayList()

    init {
        classes[Person::class.java] = IndividualPersonActivity::class.java
        classes[Repository::class.java] = RepositoryActivity::class.java
        classes[RepositoryRef::class.java] = RepositoryRefActivity::class.java
        classes[Submitter::class.java] = AuthorActivity::class.java
        classes[Change::class.java] = ChangesActivity::class.java
        classes[SourceCitation::class.java] = SourceCitationActivity::class.java
        classes[GedcomTag::class.java] = ExtensionActivity::class.java
        classes[EventFact::class.java] = EventActivity::class.java
        classes[Family::class.java] = FamilyActivity::class.java
        classes[Source::class.java] = SourceActivity::class.java
        classes[Media::class.java] = ImageActivity::class.java
        classes[Address::class.java] = AddressActivity::class.java
        classes[Name::class.java] = NameActivity::class.java
        classes[Note::class.java] = NoteActivity::class.java
    }

    class StepStack : Stack<Step>()
    class Step {
        @JvmField
        var `object`: Any? = null

        @JvmField
        var tag: String? = null

        // FindStack sets it to true then onBackPressed the stack must be deleted in bulk
        @JvmField
        var clearStackOnBackPressed = false
    }

    companion object {
        @JvmField
        var classes: MutableMap<Class<*>, Class<*>> = HashMap()
        private val memory = Memory()

        // Restituisce l'ultima pila creata se ce n'è almeno una
        // oppure ne restituisce una vuota giusto per non restituire null
        @JvmStatic
        val stepStack: StepStack
            get() = if (memory.list.size > 0) memory.list[memory.list.size - 1] else StepStack() // una pila vuota che non viene aggiunta alla lista

        @JvmStatic
        fun addStack(): StepStack {
            val stepStack = StepStack()
            memory.list.add(stepStack)
            return stepStack
        }

        /**
         * Adds the first object to a new stack
         */
        fun setFirst(`object`: Any?) {
            setFirst(`object`, null)
        }

        fun setFirst(`object`: Any?, tag: String?) {
            addStack()
            val step = add(`object`)
            if (tag != null) step.tag = tag else if (`object` is Person) step.tag = "INDI"
        }

        // Aggiunge un object alla fine dell'ultima pila esistente
        @JvmStatic
        fun add(`object`: Any?): Step {
            val step = Step()
            step.`object` = `object`
            stepStack.add(step)
            return step
        }

        /**
         * Put the first item if there are no stacks or replace the first item in the last existing stack.
         * In other words, it puts the first object without adding any more stacks
         */
        @JvmStatic
        fun replaceFirst(`object`: Any?) {
            val tag = if (`object` is Family) "FAM" else "INDI"
            if (memory.list.size == 0) {
                setFirst(`object`, tag)
            } else {
                stepStack.clear()
                val step = add(`object`)
                step.tag = tag
            }
            //stampa("replacePrimo");
        }

        /**
         * The object contained in the first step of the stack
         */
        @JvmStatic
        fun firstObject(): Any? {
            return if (stepStack.size > 0) stepStack.firstElement()!!.`object` else null
        }

        /**
         * If the stack has more than one object, get the second to last object, otherwise return null
         * The object in the previous step to the last - L'object nel passo precedente all'ultimo
         * I think it was called containerObject()?
         */
        @JvmStatic
        val secondToLastObject: Any?
            get() {
                val stepStack = stepStack
                return if (stepStack.size > 1) stepStack[stepStack.size - 2]!!.`object` else null
            }

        // L'object nell'ultimo passo
        val `object`: Any?
            get() = if (stepStack.size == 0) null else stepStack.peek()!!.`object`

        @JvmStatic
        fun clearStackAndRemove() { //lit. retreat
            while (stepStack.size > 0 && stepStack.lastElement()!!.clearStackOnBackPressed) stepStack.pop()
            if (stepStack.size > 0) stepStack.pop()
            if (stepStack.isEmpty()) memory.list.remove(stepStack)
            //stampa("arretra");
        }

        /**
         * When an object is deleted, make it null in all steps,
         * and the objects in any subsequent steps are also canceled.
         */
        @JvmStatic
        fun setInstanceAndAllSubsequentToNull(oggio: Any) {
            for (stepStack in memory.list) {
                var seguente = false
                for (step in stepStack) {
                    if (step.`object` != null && (step.`object` == oggio || seguente)) {
                        step.`object` = null
                        seguente = true
                    }
                }
            }
        }
    }
}